package com.example.recruitment.company;

public record CompanyResponse(Long id, String name, String address, String website) {
  public static CompanyResponse from(Company company) {
    return new CompanyResponse(
        company.getId(), company.getName(), company.getAddress(), company.getWebsite());
  }
}
