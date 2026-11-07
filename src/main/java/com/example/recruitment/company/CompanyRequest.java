package com.example.recruitment.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CompanyRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 500) String address,
    @NotBlank
        @Size(max = 2048)
        @URL
        @Pattern(regexp = "(?i)^https?://\\S+$", message = "must be an HTTP(S) URL")
        String website) {
  public CompanyRequest {
    name = name == null ? null : name.strip();
    address = address == null ? null : address.strip();
    website = website == null ? null : website.strip();
  }
}
