# Kết quả kiểm chứng

Ngày thực hiện: 07/11/2026.

## Môi trường và lệnh

- Java: Eclipse Temurin **17.0.15**; Maven Wrapper **3.9.11**.
- Spring Boot **3.5.16**, PostgreSQL **18.1** cài cục bộ.
- Database thử nghiệm riêng `recruitment_test`; Flyway áp dụng migration V1 và Hibernate validate schema.
- Lệnh: `./mvnw -B clean verify -Ppostgres-it` (Windows: `.\mvnw.cmd`). URL database được truyền qua `TEST_DB_URL`.

## Kết quả

| Kiểm tra | Kết quả |
| --- | --- |
| CompanyServiceTest | 6/6 |
| JobServiceTest | 10/10 |
| ApplicationServiceTest | 6/6 |
| RecruitmentApiIT | 29/29, gồm các bộ tham số |
| Tổng | **51 test, 0 lỗi, 0 bỏ qua** |
| Spotless / Google Java Format | Thành công |
| Checkstyle | 0 vi phạm |
| Executable JAR | Khởi động, migration và request HTTP thành công |

Integration test xác nhận CRUD, validation 400, not-found 404, conflict 409, phân trang/sắp xếp/bộ lọc kết hợp, wildcard dạng văn bản, unique/FK/CHECK PostgreSQL, hai request nộp trùng chỉ tạo một hồ sơ và request đợi khóa Job đóng sẽ bị từ chối.

Rà soát độc lập phát hiện chuyển enum dạng số/ID dạng thập phân và bỏ qua JSON thừa. Sáu ca HTTP tái hiện đã thất bại trước sửa (201 thay vì 400), sau đó đều thành công khi bật cấu hình Jackson nghiêm ngặt. Không còn phát hiện Critical/Important chưa xử lý; không có Minor bị hoãn.

CI cấu hình Java 17 và PostgreSQL 16 tại [GitHub Actions](https://github.com/vanh251/mini-recruitment-api/actions). Kết quả CI được hiển thị tại từng workflow run; bảng trên là kết quả đã chạy trên máy phát triển.

## Quyết định triển khai

- Làm trực tiếp trên nhánh mới trong workspace ban đầu vì chưa có mã hay Git; không tạo thêm worktree. Không có nhánh hoặc mã cũ bị thay thế. Nhánh bàn giao là `main`.
- Đặt truy vấn `hasJobs` trong CompanyRepository để kiểm tra ràng buộc xóa mà không tạo phụ thuộc vòng giữa các feature. Đây là native PostgreSQL SQL; nếu chuyển database phải kiểm tra/cập nhật truy vấn cùng migration.
- Dùng PostgreSQL cài sẵn cho kiểm chứng vì Docker daemon chưa chạy; không cần thay đổi hoặc khởi động Docker trên máy người dùng. Compose/CI vẫn sử dụng PostgreSQL 16 theo cấu hình repository.

`AGENTS.md` được giữ nguyên. Thông tin xác thực GitHub, database cục bộ, log và thư mục build không được đưa lên Git.
