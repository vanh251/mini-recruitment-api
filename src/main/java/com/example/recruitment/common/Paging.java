package com.example.recruitment.common;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class Paging {
  private Paging() {}

  public static PageRequest of(int page, int size, String sort, Set<String> fields) {
    if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
      throw ApiException.badRequest("page must be >= 0 and size must be between 1 and 100");
    }
    String[] parts = sort.split(",", -1);
    if (parts.length != 2
        || !fields.contains(parts[0])
        || !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
      throw ApiException.badRequest("sort must be an allowed field followed by ,asc or ,desc");
    }
    Sort ordering = Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
    if (!parts[0].equals("id")) ordering = ordering.and(Sort.by("id"));
    return PageRequest.of(page, size, ordering);
  }
}
