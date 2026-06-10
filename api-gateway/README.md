# API Gateway Service

Service gateway dùng Spring Cloud Gateway để route request từ client xuống các instance `news` khi mô phỏng High Availability trên local.

## Tech stack

- Java 21, Spring Boot 3.3.5
- Spring Cloud Gateway
- Spring Cloud LoadBalancer
- Spring Boot Actuator

## Flow

```text
Client -> API Gateway :8080 -> news-service :8081
                           \-> news-service :8082
```

Gateway nhận request tại `http://localhost:8080`, dùng route `lb://news-service`, sau đó Spring Cloud LoadBalancer chọn một instance `news-service` còn healthy.

## Cấu hình chính

File cấu hình nằm tại `src/main/resources/application.yml`.

Gateway mặc định:

- Chạy port `8080`.
- Route `/api/news` và `/api/news/**` xuống `news-service`.
- Khai báo 2 instance local:
  - `http://localhost:8081`
  - `http://localhost:8082`
- Health check backend qua `/actuator/health` mỗi 5 giây.
- Timeout kết nối backend 2 giây, response timeout 10 giây.
- Retry 1 lần cho request `GET` khi gặp lỗi 5xx hoặc lỗi gateway. Không retry `POST/PUT/DELETE` để tránh tạo/sửa/xóa lặp.

Có thể override bằng biến môi trường:

```bash
SERVER_PORT=8080
NEWS_SERVICE_1_URI=http://localhost:8081
NEWS_SERVICE_2_URI=http://localhost:8082
```

## Chạy local

Terminal 1: chạy MySQL, Redis và instance `news` đầu tiên.

```bash
cd news
docker compose up -d mysql redis
SERVER_PORT=8081 mvn spring-boot:run
```

Terminal 2: chạy instance `news` thứ hai.

```bash
cd news
SERVER_PORT=8082 mvn spring-boot:run
```

Terminal 3: chạy gateway.

```bash
cd api-gateway
mvn spring-boot:run
```

## Kiểm tra

Health của gateway:

```bash
curl http://localhost:8080/actuator/health
```

Danh sách route gateway:

```bash
curl http://localhost:8080/actuator/gateway/routes
```

Gọi API `news` thông qua gateway:

```bash
curl "http://localhost:8080/api/news?pageNo=0&pageSize=10"
```

Tạo tin thông qua gateway:

```bash
curl -X POST "http://localhost:8080/api/news" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Tin qua gateway",
    "summary": "Tao tu API Gateway",
    "content": "Noi dung chi tiet",
    "author": "intern",
    "status": "PUBLISHED"
  }'
```

Test failover local:

1. Start đủ `news:8081`, `news:8082`, `api-gateway:8080`.
2. Gọi `curl http://localhost:8080/api/news` vài lần.
3. Stop một instance `news`.
4. Đợi khoảng 5-10 giây để health check cập nhật.
5. Gọi lại `curl http://localhost:8080/api/news`.

Nếu còn ít nhất một instance `news` healthy, gateway vẫn route request GET sang instance còn lại.

## Build

```bash
mvn -DskipTests package
```

Chạy file jar:

```bash
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```
