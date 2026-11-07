package com.example.recruitment.job;

import com.example.recruitment.common.ApiException;
import com.example.recruitment.common.PageResponse;
import com.example.recruitment.common.Paging;
import com.example.recruitment.company.CompanyService;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobService {
  private final JobRepository jobs;
  private final CompanyService companies;

  public JobService(JobRepository jobs, CompanyService companies) {
    this.jobs = jobs;
    this.companies = companies;
  }

  @Transactional
  public JobResponse create(JobRequest request) {
    validateSalary(request);
    return JobResponse.from(
        jobs.save(new Job(request, companies.requireCompany(request.companyId()))));
  }

  public JobResponse get(Long id) {
    return JobResponse.from(jobs.findById(id).orElseThrow(() -> ApiException.notFound("Job")));
  }

  public PageResponse<JobResponse> search(
      String keyword, String location, JobStatus status, int page, int size, String sort) {
    var pageable =
        Paging.of(
            page,
            size,
            sort,
            Set.of("id", "title", "salaryFrom", "salaryTo", "location", "status"));
    return PageResponse.from(
        jobs.findAll(JobSpecifications.matching(keyword, location, status), pageable)
            .map(JobResponse::from));
  }

  @Transactional
  public JobResponse update(Long id, JobRequest request) {
    validateSalary(request);
    Job job = lockedJob(id);
    job.update(request, companies.requireCompany(request.companyId()));
    return JobResponse.from(job);
  }

  @Transactional
  public void delete(Long id) {
    Job job = lockedJob(id);
    if (jobs.hasApplications(id))
      throw ApiException.conflict("JOB_HAS_APPLICATIONS", "Job still has applications");
    jobs.delete(job);
    jobs.flush();
  }

  private Job lockedJob(Long id) {
    return jobs.findLockedById(id).orElseThrow(() -> ApiException.notFound("Job"));
  }

  private void validateSalary(JobRequest request) {
    if (!request.isSalaryRangeValid())
      throw ApiException.badRequest("salaryFrom must not exceed salaryTo");
  }
}
