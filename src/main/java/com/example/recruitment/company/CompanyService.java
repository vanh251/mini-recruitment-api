package com.example.recruitment.company;

import com.example.recruitment.common.ApiException;
import com.example.recruitment.common.PageResponse;
import com.example.recruitment.common.Paging;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyService {
  private final CompanyRepository companies;

  public CompanyService(CompanyRepository companies) {
    this.companies = companies;
  }

  @Transactional
  public CompanyResponse create(CompanyRequest request) {
    return CompanyResponse.from(
        companies.save(new Company(request.name(), request.address(), request.website())));
  }

  public CompanyResponse get(Long id) {
    return CompanyResponse.from(requireCompany(id));
  }

  public Company requireCompany(Long id) {
    return companies.findById(id).orElseThrow(() -> ApiException.notFound("Company"));
  }

  public PageResponse<CompanyResponse> list(int page, int size, String sort) {
    return PageResponse.from(
        companies
            .findAll(Paging.of(page, size, sort, Set.of("id", "name")))
            .map(CompanyResponse::from));
  }

  @Transactional
  public CompanyResponse update(Long id, CompanyRequest request) {
    Company company = requireCompany(id);
    company.update(request);
    return CompanyResponse.from(company);
  }

  @Transactional
  public void delete(Long id) {
    Company company = requireCompany(id);
    if (companies.hasJobs(id))
      throw ApiException.conflict("COMPANY_HAS_JOBS", "Company still has jobs");
    companies.delete(company);
    companies.flush();
  }
}
