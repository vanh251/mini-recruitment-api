package com.example.recruitment.job;

import java.math.BigDecimal;

public record JobResponse(
    Long id,
    String title,
    String description,
    BigDecimal salaryFrom,
    BigDecimal salaryTo,
    String location,
    JobStatus status,
    Long companyId,
    String companyName) {
  public static JobResponse from(Job job) {
    return new JobResponse(
        job.getId(),
        job.getTitle(),
        job.getDescription(),
        job.getSalaryFrom(),
        job.getSalaryTo(),
        job.getLocation(),
        job.getStatus(),
        job.getCompany().getId(),
        job.getCompany().getName());
  }
}
