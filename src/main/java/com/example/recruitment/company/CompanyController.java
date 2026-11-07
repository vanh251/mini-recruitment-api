package com.example.recruitment.company;

import com.example.recruitment.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/companies")
public class CompanyController {
  private final CompanyService service;

  public CompanyController(CompanyService service) {
    this.service = service;
  }

  @PostMapping
  ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyRequest request) {
    CompanyResponse result = service.create(request);
    return ResponseEntity.created(URI.create("/companies/" + result.id())).body(result);
  }

  @GetMapping
  PageResponse<CompanyResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "id,desc") String sort) {
    return service.list(page, size, sort);
  }

  @GetMapping("/{id}")
  CompanyResponse get(@PathVariable @Positive Long id) {
    return service.get(id);
  }

  @PutMapping("/{id}")
  CompanyResponse update(
      @PathVariable @Positive Long id, @Valid @RequestBody CompanyRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
