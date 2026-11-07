package com.example.recruitment.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.recruitment.common.ApiException;
import com.example.recruitment.company.Company;
import com.example.recruitment.job.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {
  @Mock ApplicationRepository applications;
  @Mock JobRepository jobs;
  ApplicationService service;
  Instant now = Instant.parse("2026-11-07T03:00:00Z");
  ApplicationRequest request =
      new ApplicationRequest(
          " Nguyen Van A ", " Candidate@Example.com ", "https://example.com/cv.pdf");

  @BeforeEach
  void setUp() {
    service = new ApplicationService(applications, jobs, Clock.fixed(now, ZoneOffset.UTC));
  }

  Job job(JobStatus status) {
    return new Job(
        new JobRequest("Java", "API", BigDecimal.ZERO, BigDecimal.TEN, "Ha Noi", status, 1L),
        new Company("Rikkei", "Ha Noi", "https://rikkei.vn"));
  }

  @Test
  void acceptsOpenJobAndUsesCanonicalEmailAndServerTime() {
    when(jobs.findLockedById(1L)).thenReturn(Optional.of(job(JobStatus.OPEN)));
    when(applications.saveAndFlush(any(Application.class))).thenAnswer(call -> call.getArgument(0));
    ApplicationResponse result = service.submit(1L, request);
    assertThat(result.email()).isEqualTo("candidate@example.com");
    assertThat(result.fullName()).isEqualTo("Nguyen Van A");
    assertThat(result.cvUrl()).isEqualTo("https://example.com/cv.pdf");
    assertThat(result.appliedAt()).isEqualTo(Instant.parse("2026-11-07T03:00:00Z"));
  }

  @Test
  void refusesClosedJobWithoutSaving() {
    when(jobs.findLockedById(1L)).thenReturn(Optional.of(job(JobStatus.CLOSED)));
    assertThatThrownBy(() -> service.submit(1L, request))
        .isInstanceOf(ApiException.class)
        .extracting("code")
        .isEqualTo("JOB_CLOSED");
    verify(applications, never()).saveAndFlush(any());
  }

  @Test
  void refusesDuplicateEmailIgnoringCaseAndWhitespace() {
    when(jobs.findLockedById(1L)).thenReturn(Optional.of(job(JobStatus.OPEN)));
    when(applications.existsByJobIdAndEmail(1L, "candidate@example.com")).thenReturn(true);
    assertThatThrownBy(() -> service.submit(1L, request))
        .isInstanceOf(ApiException.class)
        .extracting("code")
        .isEqualTo("DUPLICATE_APPLICATION");
    verify(applications, never()).saveAndFlush(any());
  }

  @Test
  void refusesMissingJob() {
    assertThatThrownBy(() -> service.submit(99L, request))
        .isInstanceOf(ApiException.class)
        .extracting("status.value")
        .isEqualTo(404);
    verify(applications, never()).saveAndFlush(any());
  }

  @Test
  void returnsApplicationOnlyInsideItsJob() {
    Application existing = new Application(job(JobStatus.OPEN), request, now);
    when(jobs.existsById(1L)).thenReturn(true);
    when(applications.findByIdAndJobId(2L, 1L)).thenReturn(Optional.of(existing));
    assertThat(service.get(1L, 2L).email()).isEqualTo("candidate@example.com");
  }

  @Test
  void refusesApplicationFromAnotherJob() {
    when(jobs.existsById(1L)).thenReturn(true);
    assertThatThrownBy(() -> service.get(1L, 2L))
        .isInstanceOf(ApiException.class)
        .extracting("status.value")
        .isEqualTo(404);
  }
}
