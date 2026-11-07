# Thiết kế Mini API tuyển dụng

Trạng thái: đề xuất để duyệt; chưa triển khai mã ứng dụng.

## Mục tiêu và phạm vi

Hoàn thành bài thực hành Java 17, Spring Boot 3, PostgreSQL: CRUD Company/Job, tìm kiếm Job, nộp Application, validation, lỗi JSON 400/404/409 và tối thiểu 5 unit test tầng service. Bàn giao mã nguồn, migration, README tiếng Việt và lịch sử commit nhỏ. Không thêm giao diện, đăng nhập hay gửi email vì đề không yêu cầu.

## Phương án

1. **Đề xuất: PostgreSQL + Spring Data JPA + Flyway.** Một schema migration dùng nhất quán khi chạy và kiểm thử tích hợp; khóa ngoại và unique constraint bảo vệ dữ liệu. Docker Compose hỗ trợ khởi tạo database.
2. **MySQL + Spring Data JPA + Flyway.** Cũng đáp ứng đề, phù hợp nếu người học đã có MySQL; cần đổi driver, migration và cấu hình kiểm thử.

PostgreSQL là lựa chọn do người triển khai đề xuất, chưa phải lựa chọn đã được người dùng xác nhận.

## Kiến trúc và công cụ

Maven Wrapper; Java release 17; Spring Boot dòng 3.5, phiên bản cụ thể được kiểm tra khi tạo dự án. Spring MVC, Bean Validation, JPA, Flyway, PostgreSQL JDBC; JUnit 5, Mockito và Spring Boot Test. Dùng constructor injection và DTO riêng, không trả JPA entity trực tiếp. Package gốc `com.example.recruitment`, chia theo `company`, `job`, `application` và `common`.

Source đặt tại `src/main/java`, migration tại `src/main/resources/db/migration`, test tại `src/test/java`. Cấu hình database lấy từ biến môi trường; `.env.example` chỉ chứa giá trị phát triển mẫu. Giữ nguyên `AGENTS.md` theo yêu cầu trước đó.

## Mô hình dữ liệu

- `companies`: id bigint identity, name varchar(200), address varchar(500), website varchar(2048); các trường bắt buộc, website HTTP(S).
- `jobs`: id, company_id FK, title varchar(200), description text, salary_from/salary_to numeric(15,2), location varchar(200), status varchar(6) OPEN/CLOSED. Lương không âm, salary_from <= salary_to; chỉ số company_id và (status, location).
- `applications`: id, job_id FK, full_name varchar(200), email varchar(254), cv_url varchar(2048), applied_at timestamp with time zone. Thời điểm do server tạo theo UTC. Chuẩn hóa email bằng trim + lowercase; unique(job_id, email), thêm CHECK email ở dạng chuẩn hóa.

Database có NOT NULL, CHECK và FK tương ứng validation. Từ chối xóa Company còn Job hoặc Job còn Application bằng 409 để giữ hồ sơ. Không cascade xóa dữ liệu ứng tuyển.

## Hợp đồng HTTP

- `/companies`: POST tạo (201 + Location), GET danh sách phân trang (200).
- `/companies/{id}`: GET (200), PUT cập nhật toàn bộ (200), DELETE (204).
- `/jobs`: POST tạo (201 + Location), GET tìm kiếm (200).
- `/jobs/{id}`: GET (200), PUT cập nhật toàn bộ (200), DELETE (204).
- `/jobs/{id}/applications`: POST nộp hồ sơ (201 + Location).
- `/jobs/{id}/applications/{applicationId}`: GET để truy xuất tài nguyên theo Location; xác nhận Application thuộc đúng Job.

GET `/jobs` nhận `keyword`, `location`, `status`, `page` mặc định 0, `size` mặc định 20 (1..100), `sort` mặc định `id,desc`. Keyword tìm không phân biệt hoa thường trong title/description; location tìm không phân biệt hoa thường theo chuỗi con. Kết hợp các bộ lọc bằng AND; escape ký tự `%` và `_` để tìm văn bản thực. Chỉ cho sắp xếp theo id/title/salaryFrom/salaryTo/location/status; thêm id để phân trang ổn định. Tham số sai trả 400. Response phân trang có content/page/size/totalElements/totalPages.

## Quy tắc nghiệp vụ và lỗi

Nộp hồ sơ nằm trong transaction: khóa ghi Job, kiểm tra OPEN, kiểm tra email trùng, ghi Application và flush. Cập nhật/xóa Job cũng khóa cùng bản ghi để tránh nhận hồ sơ đồng thời với đóng Job. Unique constraint vẫn là lớp bảo vệ cuối cho các request trùng.

Lỗi JSON thống nhất: `timestamp`, `status`, `code`, `message`, `path`, `fieldErrors`. Bean Validation, JSON sai định dạng, enum/id/phân trang/sort sai trả 400; tài nguyên không tồn tại trả 404; email trùng, Job CLOSED hoặc xóa bị ràng buộc trả 409. Không đưa SQL, stack trace hay thông tin kết nối vào response.

## Kiểm thử và nghiệm thu

Unit test service dùng Mockito, không cần database: CRUD và not-found, khoảng lương sai, OPEN nhận hồ sơ, CLOSED từ chối, email trùng (kể cả khác hoa thường), chuẩn hóa email và thời điểm server. Mục tiêu ít nhất 10 test để vượt mức tối thiểu 5.

Kiểm thử HTTP xác nhận validation và schema lỗi; kiểm thử PostgreSQL xác nhận migration, tổ hợp bộ lọc/phân trang/sort, unique constraint và FK. Thêm ca cạnh tranh nộp trùng/đóng Job nếu môi trường PostgreSQL chạy được. Phân biệt rõ kiểm tra đã chạy và chưa chạy trong bàn giao.

README có yêu cầu cài đặt, biến môi trường, chạy PostgreSQL bằng Compose hoặc instance sẵn có, lệnh chạy/test/build, đường dẫn migration, curl mẫu và quy tắc xóa. CI dùng Java 17. Commit theo từng phần scaffold/schema, Company, Job/search, Application/error/tests và tài liệu.

## Môi trường và nộp GitHub

Đã phát hiện JDK 17 trong `C:/Program Files/Eclipse Adoptium/jdk-17.0.15.6-hotspot`; Java mặc định là 21. Maven chưa có trong PATH; Docker CLI có nhưng daemon chưa chạy. Git đã có danh tính; GitHub kết nối tài khoản `vanh251`. Chỉ công bố repository dành cho bài này, không thay đổi repository khác. Nếu công cụ không tạo được public repo, hoàn tất và kiểm thử mã cục bộ trước, sau đó nêu chính xác thao tác còn cần để nộp.
