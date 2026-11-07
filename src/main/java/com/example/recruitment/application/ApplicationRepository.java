package com.example.recruitment.application;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
  boolean existsByJobIdAndEmail(Long jobId, String email);

  Optional<Application> findByIdAndJobId(Long id, Long jobId);
}
