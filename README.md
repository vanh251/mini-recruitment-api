# Mini API tuyển dụng

Bài thực hành **Java 17 + Spring Boot 3.5.16 + PostgreSQL**. API quản lý công ty, công việc và hồ sơ ứng tuyển; tìm kiếm có lọc/phân trang/sắp xếp; validation và lỗi JSON thống nhất. Không yêu cầu đăng nhập trong phạm vi bài.

## 1. Yêu cầu và chạy nhanh

- JDK **17**, kiểm tra `java -version` và `JAVA_HOME`.
- Docker với Docker Compose để chạy PostgreSQL; hoặc PostgreSQL 16+ cài sẵn.
- Không cần cài Maven: repository có Maven Wrapper 3.9.11. Lần chạy đầu cần Internet để tải Maven/dependencies.

### PostgreSQL qua Docker

```bash
cp .env.example .env
docker compose up -d --wait
./mvnw spring-boot:run
```

PowerShell trên Windows:

```powershell
Copy-Item .env.example .env
# Đổi đường dẫn nếu JDK 17 của bạn nằm ở nơi khác:
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot'
docker compose up -d --wait
.\mvnw.cmd spring-boot:run
```

API chạy tại `http://localhost:8080`. Database được tạo bởi container; Flyway tự chạy migration khi ứng dụng khởi động. Dừng database bằng `docker compose stop`; dữ liệu được giữ trong volume.

### PostgreSQL cài sẵn hoặc cấu hình riêng

Tạo database và role phù hợp trước, ví dụ với tài khoản quản trị PostgreSQL:

```sql
CREATE USER recruitment WITH PASSWORD 'recruitment_dev';
CREATE DATABASE recruitment OWNER recruitment;
```

| Biến môi trường cho ứng dụng | Giá trị mặc định |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/recruitment` |
| `DB_USERNAME` | `recruitment` |
| `DB_PASSWORD` | `recruitment_dev` |
| `SERVER_PORT` | `8080` |

`.env` được **Docker Compose** đọc, Spring Boot không tự đọc file này. Các giá trị mặc định khớp nhau để chạy nhanh. Khi đổi cấu hình, export biến vào shell chạy Java, ví dụ PowerShell: `$env:DB_PASSWORD = 'your-local-password'`. Các biến `POSTGRES_DB/POSTGRES_USER/POSTGRES_PASSWORD` chỉ cấu hình container khi khởi tạo volume lần đầu. Không commit mật khẩu thật.

## 2. Build, test và chất lượng mã

| Lệnh (Linux/macOS) | Mục đích |
| --- | --- |
| `./mvnw test` | Chạy unit test service, không cần database |
| `./mvnw clean verify` | Unit test, đóng gói JAR, kiểm tra format và Checkstyle |
| `./mvnw spotless:apply` | Format Java bằng Google Java Format, thụt lề 2 spaces |
| `./mvnw clean verify -Ppostgres-it` | Thêm integration test trên PostgreSQL thật |
| `java -jar target/recruitment-api-0.0.1-SNAPSHOT.jar` | Chạy JAR sau build |

Trên Windows thay `./mvnw` bằng `.\mvnw.cmd`. `*Test.java` là unit test; `*IT.java` chỉ chạy qua profile `postgres-it`. Unit test dùng JUnit 5 + Mockito; integration test dùng Spring Boot Test + MockMvc và PostgreSQL, không thay database bằng H2. Không đặt ngưỡng coverage trong đề; test tập trung vào nhánh nghiệp vụ và ràng buộc.

Tạo **database riêng, có tên kết thúc bằng `_test`**, ví dụ:

```bash
docker compose exec db createdb -U recruitment recruitment_test
./mvnw clean verify -Ppostgres-it
```

Test mặc định kết nối `jdbc:postgresql://localhost:5432/recruitment_test`, user `recruitment`, password `recruitment_dev`. Có thể đổi qua `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`. Ví dụ PowerShell:

```powershell
$env:TEST_DB_URL = 'jdbc:postgresql://localhost:5432/recruitment_test'
.\mvnw.cmd clean verify -Ppostgres-it
```

**Integration test xóa dữ liệu trong ba bảng trước mỗi ca. Chỉ trỏ tới database thử nghiệm dùng riêng.** Các ca kiểm tra gồm migration, CRUD HTTP, filter kết hợp, wildcard, validation, unique/FK, nộp đồng thời và nộp đang chờ giao dịch đóng Job. GitHub Actions chạy toàn bộ với Java 17 và PostgreSQL 16.

## 3. Cấu trúc và thiết kế

```text
src/main/java/com/example/recruitment/
  company/       # Entity, DTO, repository, service, controller
  job/           # CRUD, Specification tìm kiếm, khóa Job
  application/   # Nộp/đọc hồ sơ
  common/        # Phân trang và xử lý lỗi
src/main/resources/db/migration/V1__create_recruitment_tables.sql
src/test/java/    # Unit test và integration test
requests.http    # Request mẫu cho IDE HTTP client
```

Quan hệ: `Company 1—N Job 1—N Application`. ID là bigint identity; tiền lương `numeric(15,2)` và không âm, lương từ không vượt lương đến. Ví dụ dùng VND/tháng; API lưu số tiền và không tự quy đổi tiền tệ. Trạng thái chỉ `OPEN/CLOSED`; thời điểm nộp do server tạo ở UTC.

[Migration V1](src/main/resources/db/migration/V1__create_recruitment_tables.sql) tạo bảng, khóa ngoại, CHECK và unique `(job_id, email)`. Hibernate dùng `ddl-auto=validate`; không tự sửa schema. Không chạy lại SQL thủ công sau khi Flyway đã áp dụng; thay đổi tiếp theo cần migration V2.

DTO tách khỏi entity, constructor injection, transaction ở service. Danh sách Job dùng Specification và entity graph Company để tránh N+1. Có index cho FK và `(status, location)`; unique index hỗ trợ tra email theo Job. Tìm chuỗi con không dấu phân biệt hoa thường dùng `lower(...) LIKE` với escape `%`, `_`, `\`; B-tree không tối ưu tìm chuỗi con tùy ý. Với dữ liệu lớn có thể bổ sung `pg_trgm`/GIN bằng migration mới.

Email được trim + lowercase trước validation/lưu. Một email được nộp cho nhiều Job nhưng chỉ một lần mỗi Job. Nộp hồ sơ, sửa và xóa Job cùng khóa ghi bản ghi Job trong transaction: request đến sau khi Job đóng sẽ bị từ chối. Unique constraint bảo vệ lần cuối. Không cascade xóa: Company còn Job và Job còn Application đều trả 409 để giữ lịch sử.

## 4. API

| Method | Đường dẫn | Thành công |
| --- | --- | --- |
| POST | `/companies` | 201 + Location |
| GET | `/companies` | 200, phân trang; sort `id/name` |
| GET / PUT / DELETE | `/companies/{id}` | 200 / 200 / 204 |
| POST | `/jobs` | 201 + Location |
| GET | `/jobs` | 200, tìm kiếm/phân trang |
| GET / PUT / DELETE | `/jobs/{id}` | 200 / 200 / 204 |
| POST | `/jobs/{id}/applications` | 201 + Location |
| GET | `/jobs/{id}/applications/{applicationId}` | 200 |

PUT thay toàn bộ thông tin, tất cả trường trong payload mẫu là bắt buộc. Website và CV phải là HTTP(S) URL; CV chỉ lưu link, không tải file về.

`GET /jobs?keyword=java&location=Ha%20Noi&status=OPEN&page=0&size=20&sort=salaryFrom,desc`

- `keyword`: tìm trong tiêu đề **hoặc** mô tả; `location`: tìm chuỗi con. Không phân biệt hoa thường, có phân biệt dấu tiếng Việt. Kết hợp keyword/location/status bằng **AND**.
- `page` bắt đầu 0, `size` từ 1 đến 100, mặc định 20. Offset không vượt giới hạn số nguyên của JPA.
- `sort=field,asc|desc`, mặc định `id,desc`. Job cho phép `id`, `title`, `salaryFrom`, `salaryTo`, `location`, `status`. Nếu sort khác id, thêm id làm tiêu chí phụ để phân trang ổn định.
- Response: `{"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0}`.

### Ví dụ request

Các lệnh curl dưới đây dùng cú pháp Bash. Trên Windows có thể dùng [requests.http](requests.http) với HTTP client của IntelliJ/VS Code; thay ID nếu dữ liệu đã tồn tại.

```bash
curl -i -X POST http://localhost:8080/companies \
  -H 'Content-Type: application/json' \
  -d '{"name":"Rikkei","address":"Ha Noi","website":"https://rikkei.vn"}'

curl -i -X POST http://localhost:8080/jobs \
  -H 'Content-Type: application/json' \
  -d '{"title":"Java Developer","description":"Spring Boot REST API","salaryFrom":15000000,"salaryTo":25000000,"location":"Ha Noi","status":"OPEN","companyId":1}'

curl --get http://localhost:8080/jobs \
  --data-urlencode 'keyword=java' --data-urlencode 'location=Ha Noi' \
  --data-urlencode 'status=OPEN' --data-urlencode 'sort=salaryFrom,desc' \
  --data-urlencode 'page=0' --data-urlencode 'size=10'

curl -i -X POST http://localhost:8080/jobs/1/applications \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Nguyen Van A","email":"a@example.com","cvUrl":"https://example.com/cv.pdf"}'

curl -i -X PUT http://localhost:8080/jobs/1 \
  -H 'Content-Type: application/json' \
  -d '{"title":"Java Developer","description":"Spring Boot REST API","salaryFrom":15000000,"salaryTo":25000000,"location":"Ha Noi","status":"CLOSED","companyId":1}'
```

Nộp lại email ở cùng Job trước khi đóng trả 409 `DUPLICATE_APPLICATION`; nộp vào Job CLOSED trả 409 `JOB_CLOSED`. Trả 400 cho payload/enum/URL/email/lương/phân trang/sort sai; 404 khi tài nguyên hoặc parent không tồn tại.

```json
{
  "timestamp": "2026-11-07T03:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/jobs/1/applications",
  "fieldErrors": {"email": "must be a well-formed email address"}
}
```

Lỗi không chứa SQL, stack trace hoặc thông tin kết nối. Các mã xung đột khác: `COMPANY_HAS_JOBS`, `JOB_HAS_APPLICATIONS`, `DATA_CONFLICT` (ràng buộc database).

## 5. Đóng góp và Git

Giữ commit nhỏ theo tính năng, dùng message rõ như `feat: add company CRUD` hoặc `test: verify concurrent applications`. PR mô tả thay đổi, issue liên quan và lệnh kiểm thử đã chạy. Chạy `clean verify -Ppostgres-it` trước khi nộp; không commit `.env`, `target/` hoặc database cục bộ.
