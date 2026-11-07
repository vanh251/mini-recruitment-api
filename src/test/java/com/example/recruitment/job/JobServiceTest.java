package com.example.recruitment.job;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.recruitment.common.ApiException;
import com.example.recruitment.common.Paging;
import com.example.recruitment.company.Company;
import com.example.recruitment.company.CompanyService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {
  @Mock JobRepository jobs;
  @Mock CompanyService companies;
  JobService service;
  Company company = new Company("Rikkei", "Ha Noi", "https://rikkei.vn");

  JobRequest request(JobStatus status, String from, String to) {
    return new JobRequest(
        "Java Developer",
        "Build APIs",
        new BigDecimal(from),
        new BigDecimal(to),
        "Ha Noi",
        status,
        1L);
  }

  @BeforeEach
  void setUp() {
    service = new JobService(jobs, companies);
  }

  @Test
  void createsJobWithItsCompanyAndSalary() {
    when(companies.requireCompany(1L)).thenReturn(company);
    when(jobs.save(any(Job.class))).thenAnswer(call -> call.getArgument(0));
    JobResponse result = service.create(request(JobStatus.OPEN, "100", "200"));
    assertThat(result.title()).isEqualTo("Java Developer");
    assertThat(result.companyName()).isEqualTo("Rikkei");
    assertThat(result.salaryFrom()).isEqualByComparingTo("100");
    assertThat(result.status()).isEqualTo(JobStatus.OPEN);
  }

  @Test
  void rejectsReversedSalaryRangeBeforeSaving() {
    assertThatThrownBy(() -> service.create(request(JobStatus.OPEN, "201", "200")))
        .isInstanceOf(ApiException.class)
        .extracting("status.value")
        .isEqualTo(400);
    verify(jobs, never()).save(any());
  }

  @Test
  void missingCompanyDoesNotCreateOrphanJob() {
    when(companies.requireCompany(1L)).thenThrow(ApiException.notFound("Company"));
    assertThatThrownBy(() -> service.create(request(JobStatus.OPEN, "100", "200")))
        .isInstanceOf(ApiException.class)
        .extracting("status.value")
        .isEqualTo(404);
    verify(jobs, never()).save(any());
  }

  @Test
  void missingJobReturnsNotFound() {
    assertThatThrownBy(() -> service.get(99L))
        .isInstanceOf(ApiException.class)
        .extracting("status.value")
        .isEqualTo(404);
  }

  @Test
  void updateClosesLockedJob() {
    Job job = new Job(request(JobStatus.OPEN, "100", "200"), company);
    when(jobs.findLockedById(1L)).thenReturn(Optional.of(job));
    when(companies.requireCompany(1L)).thenReturn(company);
    JobResponse result = service.update(1L, request(JobStatus.CLOSED, "100", "300"));
    assertThat(result.status()).isEqualTo(JobStatus.CLOSED);
    assertThat(result.salaryTo()).isEqualByComparingTo("300");
  }

  @Test
  void cannotDeleteJobWithApplications() {
    when(jobs.findLockedById(1L))
        .thenReturn(Optional.of(new Job(request(JobStatus.OPEN, "100", "200"), company)));
    when(jobs.hasApplications(1L)).thenReturn(true);
    assertThatThrownBy(() -> service.delete(1L))
        .isInstanceOf(ApiException.class)
        .extracting("status.value")
        .isEqualTo(409);
    verify(jobs, never()).delete(any(Job.class));
  }

  @Test
  void deletesUnreferencedJob() {
    Job job = new Job(request(JobStatus.OPEN, "0", "0"), company);
    when(jobs.findLockedById(1L)).thenReturn(Optional.of(job));
    service.delete(1L);
    verify(jobs).delete(job);
    verify(jobs).flush();
  }

  @Test
  void rejectsUnsafeSortField() {
    assertThatThrownBy(() -> service.search(null, null, null, 0, 20, "company.name,asc"))
        .isInstanceOf(ApiException.class)
        .extracting("status.value")
        .isEqualTo(400);
  }

  @Test
  void rejectsInvalidPagingAndDirection() {
    for (int size : new int[] {0, 101}) {
      assertThatThrownBy(() -> Paging.of(0, size, "id,asc", Set.of("id")))
          .isInstanceOf(ApiException.class);
    }
    assertThatThrownBy(() -> Paging.of(-1, 20, "id,asc", Set.of("id")))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> Paging.of(0, 20, "id,invalid", Set.of("id")))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void addsIdAsStableTieBreaker() {
    var page = Paging.of(0, 5, "title,asc", Set.of("title", "id"));
    assertThat(page.getSort().stream().map(order -> order.getProperty()).toList())
        .containsExactly("title", "id");
  }
}
