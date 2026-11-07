package com.example.recruitment.job;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record JobRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 10000) String description,
    @NotNull @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal salaryFrom,
    @NotNull @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal salaryTo,
    @NotBlank @Size(max = 200) String location,
    @NotNull JobStatus status,
    @NotNull @Positive Long companyId) {
  public JobRequest {
    title = title == null ? null : title.strip();
    description = description == null ? null : description.strip();
    location = location == null ? null : location.strip();
  }

  @AssertTrue(message = "salaryFrom must not exceed salaryTo")
  public boolean isSalaryRangeValid() {
    return salaryFrom == null || salaryTo == null || salaryFrom.compareTo(salaryTo) <= 0;
  }
}
