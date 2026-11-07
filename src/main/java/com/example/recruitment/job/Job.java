package com.example.recruitment.job;

import com.example.recruitment.company.Company;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "jobs")
public class Job {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal salaryFrom;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal salaryTo;

  @Column(nullable = false, length = 200)
  private String location;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 6)
  private JobStatus status;

  protected Job() {}

  public Job(JobRequest request, Company company) {
    update(request, company);
  }

  public void update(JobRequest request, Company company) {
    this.company = company;
    title = request.title();
    description = request.description();
    salaryFrom = request.salaryFrom();
    salaryTo = request.salaryTo();
    location = request.location();
    status = request.status();
  }

  public Long getId() {
    return id;
  }

  public Company getCompany() {
    return company;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getSalaryFrom() {
    return salaryFrom;
  }

  public BigDecimal getSalaryTo() {
    return salaryTo;
  }

  public String getLocation() {
    return location;
  }

  public JobStatus getStatus() {
    return status;
  }
}
