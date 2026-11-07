# Recruitment API Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Deliver the recruitment assignment, runnable on Java 17 with PostgreSQL and documented HTTP examples.

**Architecture:** Feature packages expose DTO-based MVC controllers backed by transactional services and Spring Data repositories. Flyway owns PostgreSQL constraints; one advice produces JSON errors.

**Tech Stack:** Java 17, Spring Boot 3.5, Maven Wrapper, PostgreSQL, JPA, Flyway, JUnit 5, Mockito.

**Spec:** `docs/superpowers/specs/2026-11-07-recruitment-api-design.md` (approved by user: “triển khai luôn”).

## Global Constraints

- Preserve AGENTS.md; no authentication/UI scope.
- CRUD Company/Job; GET /jobs filters, paging and allowlisted sorting.
- Reject CLOSED jobs and duplicate email per job; standardized 400/404/409.
- At least 5 service unit tests; use Java 17 for verification.

## Review Focus

- Concurrent duplicate submissions and closing jobs must preserve invariants.
- Missing parent and delete conflicts must produce JSON 404/409.
- Invalid JSON, sort, enum and pagination must return JSON 400.
- Salary precision/order and literal search wildcards must behave predictably.
- Flyway constraints must match JPA mappings on real PostgreSQL.

### Task 1: Build and schema

Files: `pom.xml`, Maven Wrapper, `compose.yaml`, `src/main/resources/application.yml`, `db/migration/V1__create_recruitment_tables.sql`.
- [x] Configure Maven/Java 17, dependency versions, formatting and CI.
- [x] Add database schema with FK, CHECK, unique and search indexes.
- [x] Verify wrapper and dependency resolution; commit scaffold.

### Task 2: Company CRUD

Files: `company/{Company,CompanyRequest,CompanyResponse,CompanyRepository,CompanyService,CompanyController}.java`; common paging/errors; `CompanyServiceTest.java`.
Interface: create/update/get return CompanyResponse; list returns PageResponse; delete returns void. CompanyService.requireCompany(Long) returns Company for Job service.
- [x] Write service tests for create/get/update/delete/not-found and deletion conflict; observe RED.
- [x] Implement DTO validation, CRUD service/controller, response mapping and error advice.
- [x] Run service suite GREEN; commit Company feature.

### Task 3: Job CRUD and search

Files: `job/{Job,JobStatus,JobRequest,JobResponse,JobRepository,JobService,JobController,JobSpecifications}.java`; `JobServiceTest.java`.
Interface: JobRepository.findLockedById(Long) locks for write; JobService.search returns PageResponse<JobResponse>; create/update/get return JobResponse.
- [x] Write tests for salary range, missing company/job, filters and safe sort; observe RED.
- [x] Implement CRUD, literal escaped case-insensitive search, paging and locking.
- [x] Run complete suite GREEN; commit Job feature.

### Task 4: Applications

Files: `application/{Application,ApplicationRequest,ApplicationResponse,ApplicationRepository,ApplicationService,ApplicationController}.java`; `ApplicationServiceTest.java`.
Interface: submit(Long,ApplicationRequest) and get(Long,Long) return ApplicationResponse; inject Clock for deterministic server time.
- [x] Test OPEN success, CLOSED rejection, duplicate rejection, canonical email, server timestamp, missing job, constraint conflict; observe RED.
- [x] Implement transactional submit with locked job, normalized email and saveAndFlush.
- [x] Run complete suite GREEN; commit Application feature.

### Task 5: HTTP/database verification and delivery

Files: `RecruitmentApiIntegrationTest.java`, `README.md`, `requests.http`, `.github/workflows/ci.yml`.
- [x] Write full-stack HTTP tests for validation/status/errors/search and PostgreSQL constraints/races.
- [x] Run migration/integration tests against PostgreSQL and Java 17; fix evidenced failures.
- [x] Document commands, endpoint payloads, schema and tradeoffs; run formatter and clean verify.
- [x] Obtain whole-project independent review, fix material findings, verify again and commit.
- [x] Publish a new public repository if authenticated creation is available; otherwise report exact outstanding step without claiming publication.
