package com.example.recruitment.company;

import jakarta.persistence.*;

@Entity
@Table(name = "companies")
public class Company {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 500)
  private String address;

  @Column(nullable = false, length = 2048)
  private String website;

  protected Company() {}

  public Company(String name, String address, String website) {
    this.name = name;
    this.address = address;
    this.website = website;
  }

  public void update(CompanyRequest request) {
    name = request.name();
    address = request.address();
    website = request.website();
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  public String getWebsite() {
    return website;
  }
}
