package com.example.recruitment.application;

import java.time.Instant;

public record ApplicationResponse(
    Long id, Long jobId, String fullName, String email, String cvUrl, Instant appliedAt) {
  public static ApplicationResponse from(Application application) {
    return new ApplicationResponse(
        application.getId(),
        application.getJob().getId(),
        application.getFullName(),
        application.getEmail(),
        application.getCvUrl(),
        application.getAppliedAt());
  }
}
