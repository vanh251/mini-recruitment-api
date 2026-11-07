package com.example.recruitment.job;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class JobSpecifications {
  private JobSpecifications() {}

  public static Specification<Job> matching(String keyword, String location, JobStatus status) {
    return (root, query, cb) -> {
      List<Predicate> filters = new ArrayList<>();
      if (keyword != null && !keyword.isBlank()) {
        String pattern = contains(keyword);
        filters.add(
            cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("description")), pattern, '\\')));
      }
      if (location != null && !location.isBlank()) {
        filters.add(cb.like(cb.lower(root.get("location")), contains(location), '\\'));
      }
      if (status != null) filters.add(cb.equal(root.get("status"), status));
      return cb.and(filters.toArray(Predicate[]::new));
    };
  }

  private static String contains(String input) {
    return "%"
        + input
            .strip()
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        + "%";
  }
}
