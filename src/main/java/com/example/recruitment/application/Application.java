package com.example.recruitment.application;

import com.example.recruitment.job.Job;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "applications",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_applications_job_email",
            columnNames = {"job_id", "email"}))
public class Application {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "job_id", nullable = false)
  private Job job;

  @Column(nullable = false, length = 200)
  private String fullName;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(nullable = false, length = 2048)
  private String cvUrl;

  @Column(nullable = false)
  private Instant appliedAt;

  protected Application() {}

  public Application(Job job, ApplicationRequest request, Instant appliedAt) {
    this.job = job;
    fullName = request.fullName();
    email = request.email();
    cvUrl = request.cvUrl();
    this.appliedAt = appliedAt;
  }

  public Long getId() {
    return id;
  }

  public Job getJob() {
    return job;
  }

  public String getFullName() {
    return fullName;
  }

  public String getEmail() {
    return email;
  }

  public String getCvUrl() {
    return cvUrl;
  }

  public Instant getAppliedAt() {
    return appliedAt;
  }
}
