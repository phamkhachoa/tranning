# News Service

Service quản lý tin tức bằng Java 21 + Spring Boot 3, cấu trúc tham khảo theo một backend service của [yas](https://github.com/nashtech-garage/yas) (controller / service / repository / model / dto / exception / config / utils — yas gọi package dto là `viewmodel`).

## Tech stack

- Java 21, Spring Boot 3.3 (Web, Data JPA, Validation, Cache, Actuator)
- PostgreSQL 16 (JPA + Liquibase migration)
- Redis 7 (cache chi tiết tin tức, TTL 10 phút)
- Lombok + MapStruct
- Springdoc OpenAPI (Swagger UI)
- Docker / Docker Compose

## Cấu trúc

```
src/main/java/com/onemount/news/
├── NewsApplication.java
├── config/          # RedisConfig (cache manager), JpaAuditingConfig
├── controller/      # NewsController (REST API)
├── exception/       # NotFoundException, BadRequestException, ApiExceptionHandler
├── mapper/          # NewsMapper (MapStruct)
├── model/           # News entity, AbstractAuditEntity, enumeration/
├── repository/      # NewsRepository (Spring Data JPA)
├── service/         # NewsService (business logic + cache)
├── utils/           # Constants (error code)
└── dto/             # BaseResponse (wrapper chung), NewsRequest, NewsResponse, NewsSummaryResponse, NewsListResponse
```

## Chạy bằng Docker Compose (cả app + PostgreSQL + Redis)

```bash
docker compose up -d --build
```

## Chạy local (chỉ PostgreSQL + Redis trong Docker)

```bash
docker compose up -d postgres redis
mvn spring-boot:run
```

Yêu cầu JDK 21. Cấu hình mặc định trỏ tới `localhost:5432` (user `news` / pass `news123`, database `news`) và `localhost:6379`, có thể override bằng biến môi trường (`NEWS_DB_HOST`, `NEWS_DB_PORT`, `NEWS_DB_NAME`, `NEWS_DB_USERNAME`, `NEWS_DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`).

Liquibase sẽ chạy changelog tại `src/main/resources/db/changelog/db.changelog.xml` khi project start để khởi tạo bảng `news` ban đầu. Hibernate DDL đã được tắt bằng `spring.jpa.hibernate.ddl-auto=none`.

## API

Swagger UI: http://localhost:8080/swagger-ui.html

| Method | Path             | Mô tả                                  |
|--------|------------------|----------------------------------------|
| GET    | `/api/news`      | TODO: danh sách tin, nhận `keyword`, phân trang `pageNo`, `pageSize`, search gần đúng theo tiêu đề |
| GET    | `/api/news/{id}` | TODO: chi tiết tin, áp dụng cache Redis |
| POST   | `/api/news`      | Tạo tin mới (HTTP 201)                 |
| PUT    | `/api/news/{id}` | TODO: cập nhật tin, trả về tin sau khi sửa, cập nhật cache |
| DELETE | `/api/news/{id}` | TODO: xoá tin, evict cache             |

Hiện tại chỉ API create đã có full flow implementation. Các API còn lại đang trả `501 Not Implemented` và có TODO trong `NewsService` để thực tập sinh hoàn thiện.

Mọi API đều trả về cấu trúc chung `BaseResponse`, DTO nằm trong field `data`:

```json
{ "code": 200, "message": "Success", "data": { ... } }
```

Khi lỗi, `data` không có, thông tin lỗi nằm ở `message` (và `errors` với lỗi validation):

```json
{ "code": 400, "message": "Request information is not valid", "errors": ["title must not be blank"] }
```

Ví dụ tạo tin:

```bash
curl -X POST http://localhost:8080/api/news \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Tin tức đầu tiên",
    "summary": "Tóm tắt ngắn",
    "content": "Nội dung chi tiết...",
    "author": "hoapham",
    "status": "PUBLISHED"
  }'
```

`status` nhận các giá trị: `DRAFT`, `PUBLISHED`, `ARCHIVED`. `slug` nếu không truyền sẽ tự sinh từ `title`.
