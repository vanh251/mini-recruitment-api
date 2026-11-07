package com.example.recruitment.job;

import com.example.recruitment.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jobs")
public class JobController {
  private final JobService service;

  public JobController(JobService service) {
    this.service = service;
  }

  @PostMapping
  ResponseEntity<JobResponse> create(@Valid @RequestBody JobRequest request) {
    JobResponse result = service.create(request);
    return ResponseEntity.created(URI.create("/jobs/" + result.id())).body(result);
  }

  @GetMapping
  PageResponse<JobResponse> search(
      @RequestParam(required = false) @Size(max = 200) String keyword,
      @RequestParam(required = false) @Size(max = 200) String location,
      @RequestParam(required = false) JobStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id,desc") String sort) {
    return service.search(keyword, location, status, page, size, sort);
  }

  @GetMapping("/{id}")
  JobResponse get(@PathVariable @Positive Long id) {
    return service.get(id);
  }

  @PutMapping("/{id}")
  JobResponse update(@PathVariable @Positive Long id, @Valid @RequestBody JobRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
