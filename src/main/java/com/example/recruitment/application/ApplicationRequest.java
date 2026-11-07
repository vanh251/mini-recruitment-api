package com.example.recruitment.application;

import jakarta.validation.constraints.*;
import java.util.Locale;
import org.hibernate.validator.constraints.URL;

public record ApplicationRequest(
    @NotBlank @Size(max = 200) String fullName,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank
        @Size(max = 2048)
        @URL
        @Pattern(regexp = "(?i)^https?://\\S+$", message = "must be an HTTP(S) URL")
        String cvUrl) {
  public ApplicationRequest {
    fullName = fullName == null ? null : fullName.strip();
    email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    cvUrl = cvUrl == null ? null : cvUrl.strip();
  }
}
