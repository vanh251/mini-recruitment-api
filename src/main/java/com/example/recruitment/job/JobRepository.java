package com.example.recruitment.job;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select j from Job j where j.id = :id")
  Optional<Job> findLockedById(@Param("id") Long id);

  @Override
  @EntityGraph(attributePaths = "company")
  Page<Job> findAll(Specification<Job> specification, Pageable pageable);

  @Query(value = "SELECT EXISTS(SELECT 1 FROM applications WHERE job_id = :id)", nativeQuery = true)
  boolean hasApplications(@Param("id") Long id);
}
