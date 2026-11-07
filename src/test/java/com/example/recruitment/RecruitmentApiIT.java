package com.example.recruitment;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.recruitment.job.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecruitmentApiIT {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired JobRepository jobs;
  @Autowired JobService jobService;
  @Autowired PlatformTransactionManager transactionManager;

  @BeforeEach
  void resetDisposableDatabase() {
    String database = jdbc.queryForObject("select current_database()", String.class);
    if (database == null || !database.endsWith("_test")) {
      throw new IllegalStateException(
          "Integration tests require a disposable database ending in _test");
    }
    jdbc.execute("TRUNCATE applications, jobs, companies RESTART IDENTITY CASCADE");
  }

  @Test
  void trailingJsonDocumentsAreRejected() throws Exception {
    mvc.perform(
            post("/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(companyBody("Rikkei")) + " {}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    assertThat(jdbc.queryForObject("select count(*) from companies", Integer.class)).isZero();
  }

  static Stream<Arguments> invalidScalarTypes() {
    return Stream.of(
        Arguments.of("status", 0),
        Arguments.of("status", 1),
        Arguments.of("status", "0"),
        Arguments.of("status", "1"),
        Arguments.of("companyId", 1.9));
  }

  @ParameterizedTest
  @MethodSource("invalidScalarTypes")
  void invalidJobScalarTypesAreRejected(String field, Object value) throws Exception {
    var payload = jobBody(company(), "Java", "API", "Ha Noi", "OPEN", 100);
    payload.put(field, value);
    mvc.perform(
            post("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    assertThat(jdbc.queryForObject("select count(*) from jobs", Integer.class)).isZero();
  }

  @Test
  void protocolErrorsKeepTheirHttpStatusAndJsonShape() throws Exception {
    mvc.perform(patch("/companies/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(405));
    mvc.perform(post("/companies").contentType(MediaType.TEXT_PLAIN).content("hello"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.status").value(415));
  }

  @Test
  void companyCrudIncludesLocationsPagingAndDeletion() throws Exception {
    var created =
        mvc.perform(
                post("/companies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(companyBody("Rikkei"))))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/companies/1"))
            .andExpect(jsonPath("$.name").value("Rikkei"))
            .andReturn();
    long id = body(created).path("id").asLong();
    mvc.perform(get("/companies/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.address").value("Ha Noi"));
    mvc.perform(
            put("/companies/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(companyBody("Updated"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated"));
    mvc.perform(get("/companies").param("sort", "name,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Updated"));
    mvc.perform(delete("/companies/{id}", id))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    mvc.perform(get("/companies/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void jobCrudPersistsUpdatesAndPreventsDeletingReferencedCompany() throws Exception {
    long company = company();
    long job = job(company, "Java", "Build APIs", "Ha Noi", "OPEN", 100);
    mvc.perform(get("/jobs/{id}", job))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyId").value(company))
        .andExpect(jsonPath("$.companyName").value("Rikkei"));
    var changed = jobBody(company, "Senior Java", "Build APIs", "Da Nang", "CLOSED", 200);
    mvc.perform(
            put("/jobs/{id}", job)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(changed)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"))
        .andExpect(jsonPath("$.location").value("Da Nang"));
    mvc.perform(delete("/companies/{id}", company))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COMPANY_HAS_JOBS"));
    mvc.perform(delete("/jobs/{id}", job)).andExpect(status().isNoContent());
    mvc.perform(get("/jobs/{id}", job)).andExpect(status().isNotFound());
    mvc.perform(delete("/companies/{id}", company)).andExpect(status().isNoContent());
  }

  @Test
  void searchCombinesFiltersSortAndStablePagination() throws Exception {
    long company = company();
    job(company, "Engineer A", "JAVA and Spring", "Ha Noi", "OPEN", 100);
    long second = job(company, "Java B", "REST", "HA NOI", "OPEN", 200);
    job(company, "Java C", "REST", "Da Nang", "OPEN", 300);
    job(company, "Java D", "REST", "Ha Noi", "CLOSED", 400);
    mvc.perform(
            get("/jobs")
                .param("keyword", "java")
                .param("location", "ha noi")
                .param("status", "OPEN")
                .param("page", "1")
                .param("size", "1")
                .param("sort", "salaryFrom,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.content[0].id").value(second));
    mvc.perform(get("/jobs").param("page", "99"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(4));
  }

  @Test
  void searchesWildcardsAsLiteralCharacters() throws Exception {
    long company = company();
    long target = job(company, "100% Java_Developer\\team", "API", "Ha_Noi", "OPEN", 100);
    job(company, "Normal Java Developer", "API", "Ha Noi", "OPEN", 100);
    for (String term : List.of("%", "_", "\\")) {
      mvc.perform(get("/jobs").param("keyword", term))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.content[0].id").value(target));
    }
    mvc.perform(get("/jobs").param("location", "_"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  static Stream<Arguments> invalidParameters() {
    return Stream.of(
        Arguments.of("page", "-1"),
        Arguments.of("size", "0"),
        Arguments.of("size", "101"),
        Arguments.of("sort", "salaryFrom,up"),
        Arguments.of("sort", "company.name,asc"),
        Arguments.of("sort", "id"),
        Arguments.of("status", "PAUSED"),
        Arguments.of("page", "abc"),
        Arguments.of("keyword", "x".repeat(201)),
        Arguments.of("page", "2147483647"));
  }

  @ParameterizedTest
  @MethodSource("invalidParameters")
  void invalidSearchParametersReturnUniform400(String key, String value) throws Exception {
    mvc.perform(get("/jobs").param(key, value))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.code").isString())
        .andExpect(jsonPath("$.message").isString())
        .andExpect(jsonPath("$.path").value("/jobs"))
        .andExpect(jsonPath("$.fieldErrors").isMap());
  }

  @Test
  void validationRejectsEmptyCompanyAndMalformedJson() throws Exception {
    mvc.perform(
            post("/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\" \",\"address\":\"Ha Noi\",\"website\":\"ftp://example.com\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fieldErrors.name").exists())
        .andExpect(jsonPath("$.fieldErrors.website").exists());
    mvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    mvc.perform(get("/jobs/-1")).andExpect(status().isBadRequest());
    mvc.perform(get("/jobs/not-a-number")).andExpect(status().isBadRequest());
    mvc.perform(get("/unknown"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void validatesSalaryPrecisionRangeAndUnknownStatus() throws Exception {
    long company = company();
    for (Object invalidSalary : List.of(-1, 200.001, "10000000000000")) {
      var payload = jobBody(company, "Java", "API", "Ha Noi", "OPEN", 100);
      payload.put("salaryFrom", invalidSalary);
      mvc.perform(
              post("/jobs")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsString(payload)))
          .andExpect(status().isBadRequest());
    }
    var payload = jobBody(company, "Java", "API", "Ha Noi", "OPEN", 100);
    payload.put("salaryFrom", 999);
    mvc.perform(
            post("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.salaryRangeValid").exists());
    payload.put("status", "INVALID");
    mvc.perform(
            post("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsMissingCompanyAndMissingJob() throws Exception {
    mvc.perform(
            post("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(jobBody(999, "Java", "API", "Ha Noi", "OPEN", 100))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.path").value("/jobs"));
    mvc.perform(
            post("/jobs/999/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(applicationBody("a@example.com")))
        .andExpect(status().isNotFound());
  }

  @Test
  void applicationNormalizesEmailPreventsDuplicatesAndProtectsHistory() throws Exception {
    long company = company();
    long job = job(company, "Java", "API", "Ha Noi", "OPEN", 100);
    Instant before = Instant.now().minusSeconds(1);
    var response =
        mvc.perform(
                post("/jobs/{id}/applications", job)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(applicationBody(" Candidate@Example.com ")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/jobs/1/applications/1"))
            .andExpect(jsonPath("$.email").value("candidate@example.com"))
            .andExpect(jsonPath("$.jobId").value(job))
            .andReturn();
    Instant appliedAt = Instant.parse(body(response).path("appliedAt").asText());
    assertThat(appliedAt).isAfter(before).isBefore(Instant.now().plusSeconds(1));
    mvc.perform(get(response.getResponse().getHeader("Location")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("candidate@example.com"));
    mvc.perform(
            post("/jobs/{id}/applications", job)
                .contentType(MediaType.APPLICATION_JSON)
                .content(applicationBody("candidate@example.com")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DUPLICATE_APPLICATION"));
    mvc.perform(delete("/jobs/{id}", job))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("JOB_HAS_APPLICATIONS"));
    long other = job(company, "Other", "API", "Ha Noi", "OPEN", 100);
    mvc.perform(get("/jobs/{job}/applications/1", other)).andExpect(status().isNotFound());
    mvc.perform(
            post("/jobs/{id}/applications", other)
                .contentType(MediaType.APPLICATION_JSON)
                .content(applicationBody("candidate@example.com")))
        .andExpect(status().isCreated());
  }

  @Test
  void closedJobAndInvalidApplicationAreRejected() throws Exception {
    long job = job(company(), "Java", "API", "Ha Noi", "CLOSED", 100);
    mvc.perform(
            post("/jobs/{id}/applications", job)
                .contentType(MediaType.APPLICATION_JSON)
                .content(applicationBody("a@example.com")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("JOB_CLOSED"));
    mvc.perform(
            post("/jobs/{id}/applications", job)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"fullName\":\"\",\"email\":\"wrong\",\"cvUrl\":\"javascript:alert(1)\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.email").exists())
        .andExpect(jsonPath("$.fieldErrors.cvUrl").exists())
        .andExpect(jsonPath("$.fieldErrors.fullName").exists());
  }

  @Test
  void databaseEnforcesUniqueEmailNormalizationSalaryAndForeignKeys() throws Exception {
    long company = company();
    long job = job(company, "Java", "API", "Ha Noi", "OPEN", 100);
    jdbc.update(
        "INSERT INTO applications(job_id, full_name, email, cv_url, applied_at) VALUES (?, 'Candidate', 'a@example.com', 'https://example.com/cv', now())",
        job);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO applications(job_id, full_name, email, cv_url, applied_at) VALUES (?, 'Other', 'a@example.com', 'https://example.com/cv', now())",
                    job))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("UPDATE applications SET email = 'A@example.com'"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("UPDATE jobs SET salary_to = 0 WHERE id = ?", job))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("DELETE FROM companies WHERE id = ?", company))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("DELETE FROM jobs WHERE id = ?", job))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void concurrentDuplicateSubmissionsCreateExactlyOneApplication() throws Exception {
    long job = job(company(), "Java", "API", "Ha Noi", "OPEN", 100);
    var executor = Executors.newFixedThreadPool(2);
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    try {
      Callable<Integer> submit =
          () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return mvc.perform(
                    post("/jobs/{id}/applications", job)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody("same@example.com")))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var first = executor.submit(submit);
      var second = executor.submit(submit);
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(201, 409);
      assertThat(jdbc.queryForObject("select count(*) from applications", Integer.class))
          .isEqualTo(1);
    } finally {
      start.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void applicationWaitingForClosingTransactionSeesClosedStatus() throws Exception {
    long company = company();
    long job = job(company, "Java", "API", "Ha Noi", "OPEN", 100);
    var executor = Executors.newSingleThreadExecutor();
    var transaction = new TransactionTemplate(transactionManager);
    try {
      Future<Integer> pending =
          transaction.execute(
              tx -> {
                jobs.findLockedById(job).orElseThrow();
                var result =
                    executor.submit(
                        () ->
                            mvc.perform(
                                    post("/jobs/{id}/applications", job)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(applicationBody("waiting@example.com")))
                                .andReturn()
                                .getResponse()
                                .getStatus());
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                boolean waiting = false;
                while (System.nanoTime() < deadline) {
                  Integer count =
                      jdbc.queryForObject(
                          "select count(*) from pg_stat_activity where datname=current_database() and wait_event_type='Lock'",
                          Integer.class);
                  if (count != null && count > 0) {
                    waiting = true;
                    break;
                  }
                  try {
                    Thread.sleep(20);
                  } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                  }
                }
                assertThat(waiting).as("submission must wait for the Job row lock").isTrue();
                jobService.update(
                    job,
                    new JobRequest(
                        "Java",
                        "API",
                        java.math.BigDecimal.valueOf(100),
                        java.math.BigDecimal.valueOf(200),
                        "Ha Noi",
                        JobStatus.CLOSED,
                        company));
                return result;
              });
      assertThat(pending).isNotNull();
      assertThat(pending.get(10, TimeUnit.SECONDS)).isEqualTo(409);
      assertThat(jdbc.queryForObject("select count(*) from applications", Integer.class)).isZero();
    } finally {
      executor.shutdownNow();
    }
  }

  private Map<String, Object> companyBody(String name) {
    return Map.of("name", name, "address", "Ha Noi", "website", "https://rikkei.vn");
  }

  private Map<String, Object> jobBody(
      long company, String title, String description, String location, String status, int salary) {
    return new LinkedHashMap<>(
        Map.of(
            "companyId",
            company,
            "title",
            title,
            "description",
            description,
            "salaryFrom",
            salary,
            "salaryTo",
            salary + 100,
            "location",
            location,
            "status",
            status));
  }

  private long company() throws Exception {
    MvcResult result =
        mvc.perform(
                post("/companies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(companyBody("Rikkei"))))
            .andExpect(status().isCreated())
            .andReturn();
    return body(result).path("id").asLong();
  }

  private long job(
      long company, String title, String description, String location, String status, int salary)
      throws Exception {
    MvcResult result =
        mvc.perform(
                post("/jobs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            jobBody(company, title, description, location, status, salary))))
            .andExpect(status().isCreated())
            .andReturn();
    return body(result).path("id").asLong();
  }

  private String applicationBody(String email) throws Exception {
    return json.writeValueAsString(
        Map.of("fullName", "Nguyen Van A", "email", email, "cvUrl", "https://example.com/cv.pdf"));
  }

  private JsonNode body(MvcResult result) throws Exception {
    return json.readTree(result.getResponse().getContentAsString());
  }
}
