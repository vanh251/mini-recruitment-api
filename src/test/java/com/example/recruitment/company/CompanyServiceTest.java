package com.example.recruitment.company;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.recruitment.common.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {
  @Mock CompanyRepository companies;
  CompanyService service;
  Company company;
  CompanyRequest request = new CompanyRequest("Rikkei", "Ha Noi", "https://rikkei.vn");

  @BeforeEach
  void setUp() {
    service = new CompanyService(companies);
    company = new Company("Old", "Old address", "https://old.example");
  }

  @Test
  void createsCompanyFromRequest() {
    when(companies.save(any(Company.class))).thenAnswer(call -> call.getArgument(0));
    CompanyResponse result = service.create(request);
    assertThat(result.name()).isEqualTo("Rikkei");
    assertThat(result.address()).isEqualTo("Ha Noi");
    assertThat(result.website()).isEqualTo("https://rikkei.vn");
  }

  @Test
  void returnsExistingCompany() {
    when(companies.findById(1L)).thenReturn(Optional.of(company));
    assertThat(service.get(1L).name()).isEqualTo("Old");
  }

  @Test
  void rejectsMissingCompany() {
    assertThatThrownBy(() -> service.get(99L))
        .isInstanceOf(ApiException.class)
        .extracting("status.value")
        .isEqualTo(404);
  }

  @Test
  void updatesAllCompanyFields() {
    when(companies.findById(1L)).thenReturn(Optional.of(company));
    CompanyResponse result = service.update(1L, request);
    assertThat(result.name()).isEqualTo("Rikkei");
    assertThat(result.address()).isEqualTo("Ha Noi");
    assertThat(result.website()).isEqualTo("https://rikkei.vn");
  }

  @Test
  void refusesDeletionWhenCompanyHasJobs() {
    when(companies.findById(1L)).thenReturn(Optional.of(company));
    when(companies.hasJobs(1L)).thenReturn(true);
    assertThatThrownBy(() -> service.delete(1L))
        .isInstanceOf(ApiException.class)
        .extracting("status.value")
        .isEqualTo(409);
    verify(companies, never()).delete(any());
  }

  @Test
  void deletesCompanyWithoutJobs() {
    when(companies.findById(1L)).thenReturn(Optional.of(company));
    service.delete(1L);
    verify(companies).delete(company);
    verify(companies).flush();
  }
}
