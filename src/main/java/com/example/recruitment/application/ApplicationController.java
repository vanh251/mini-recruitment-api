package com.example.recruitment.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jobs/{jobId}/applications")
public class ApplicationController {
  private final ApplicationService service;

  public ApplicationController(ApplicationService service) {
    this.service = service;
  }

  @PostMapping
  ResponseEntity<ApplicationResponse> submit(
      @PathVariable @Positive Long jobId, @Valid @RequestBody ApplicationRequest request) {
    ApplicationResponse result = service.submit(jobId, request);
    return ResponseEntity.created(URI.create("/jobs/" + jobId + "/applications/" + result.id()))
        .body(result);
  }

  @GetMapping("/{applicationId}")
  ApplicationResponse get(
      @PathVariable @Positive Long jobId, @PathVariable @Positive Long applicationId) {
    return service.get(jobId, applicationId);
  }
}
