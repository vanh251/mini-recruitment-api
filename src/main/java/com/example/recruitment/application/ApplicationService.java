package com.example.recruitment.application;

import com.example.recruitment.common.ApiException;
import com.example.recruitment.job.Job;
import com.example.recruitment.job.JobRepository;
import com.example.recruitment.job.JobStatus;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApplicationService {
  private final ApplicationRepository applications;
  private final JobRepository jobs;
  private final Clock clock;

  public ApplicationService(ApplicationRepository applications, JobRepository jobs, Clock clock) {
    this.applications = applications;
    this.jobs = jobs;
    this.clock = clock;
  }

  @Transactional
  public ApplicationResponse submit(Long jobId, ApplicationRequest request) {
    Job job = jobs.findLockedById(jobId).orElseThrow(() -> ApiException.notFound("Job"));
    if (job.getStatus() == JobStatus.CLOSED)
      throw ApiException.conflict("JOB_CLOSED", "Job is closed for applications");
    if (applications.existsByJobIdAndEmail(jobId, request.email())) {
      throw ApiException.conflict("DUPLICATE_APPLICATION", "Email has already applied to this job");
    }
    return ApplicationResponse.from(
        applications.saveAndFlush(new Application(job, request, clock.instant())));
  }

  public ApplicationResponse get(Long jobId, Long applicationId) {
    if (!jobs.existsById(jobId)) throw ApiException.notFound("Job");
    return ApplicationResponse.from(
        applications
            .findByIdAndJobId(applicationId, jobId)
            .orElseThrow(() -> ApiException.notFound("Application")));
  }
}
