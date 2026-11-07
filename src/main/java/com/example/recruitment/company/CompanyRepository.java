package com.example.recruitment.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {
  @Query(value = "SELECT EXISTS(SELECT 1 FROM jobs WHERE company_id = :id)", nativeQuery = true)
  boolean hasJobs(@Param("id") Long id);
}
