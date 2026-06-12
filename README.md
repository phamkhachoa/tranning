# Training: News Service HA, API Gateway va Identity Service

Repo nay dung de huong dan thuc tap sinh xay he thong tu dau theo tung buoc. Folder `news`
la bai HA/API Gateway ban dau. Hai folder moi `identity-service` va `api-gateway` la bai hoc
ve Spring Security, JWT, request routing va cach tach identity ra mot service rieng.

## Identity training nhanh

Thu tu hoc de nghi:

1. Doc `identity-service/README.md` de hieu endpoint, data model va service/repository TODO.
2. Chay infra chung bang `docker compose up -d news-postgres news-redis identity-postgres`.
3. Chay `identity-service` truc tiep tren port `8081`, test `POST /users`.
4. Doc service methods chua implement nhu `AuthService.login`, `AuthService.refresh`, `JwtTokenService.createAccessToken`.
5. Mo rong Liquibase migration cho roles, permissions, refresh token va audit log.
6. Them RBAC day du: role, permission, assignment theo scope.

Flow tong quat:

```text
Client -> API Gateway -> Identity Service
                 |              |
                 |              -> AuthService / UserService / AuthzService
                 |              -> Repository
                 -> verify JWT va forward X-User-* headers
```

Request mau:

```bash
# Terminal 1
docker compose up -d news-postgres news-redis identity-postgres

# Terminal 2
cd identity-service
mvn spring-boot:run

# Terminal 3
cd api-gateway
mvn spring-boot:run

# Terminal 4
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{"email":"student1@courseflow.local","password":"Student@123","displayName":"Student One"}'
```

## Infra chung

Root `docker-compose.yml` dung de chay cac dependency local cho cac bai training:

```bash
docker compose up -d news-postgres news-redis identity-postgres
```

Service infra:

| Service | Port host | Dung cho |
| --- | --- | --- |
| `news-postgres` | `5432` | database rieng cho `news` service |
| `news-redis` | `6379` | cache cho `news` service |
| `identity-postgres` | `5433` | database rieng cho `identity-service` |

## Liquibase convention

Ca `news` va `identity-service` dung cung convention voi CourseFlow:

```text
src/main/resources/db/changelog/db.changelog.xml
src/main/resources/db/changelog/changes/001-init.sql
```

Them migration moi bang cach tao `changes/NNN-ten-thay-doi.sql` va include file do trong `db.changelog.xml`.

Repo nay dang co service `news` viet bang Java 21 + Spring Boot 3. Muc tieu tiep theo la mo phong High Availability (HA) tren local bang cach chay 2 instance `news`, sau do them mot service `api-gateway` dung Spring Cloud Gateway de route request xuong cac instance `news`.

## 1. HA hoat dong nhu the nao?

HA (High Availability) la cach thiet ke he thong de service van tiep tuc phuc vu request khi mot phan cua he thong gap loi. Thay vi chi co 1 instance duy nhat, ta chay nhieu instance giong nhau va dat mot lop dieu phoi request o phia truoc.

Luon tu duy theo flow:

```text
Client -> API Gateway / Load Balancer -> News instance 1
                                    \-> News instance 2
                                    \-> News instance N

News instances -> PostgreSQL / Redis / external dependencies
```

Nhung thanh phan quan trong:

- **Nhieu instance cua cung mot service**: cac instance `news` cung chay mot codebase, nhung lang nghe tren cac port hoac host khac nhau.
- **Load balancing**: API Gateway hoac Load Balancer chia request xuong cac instance dang san sang nhan request.
- **Health check**: he thong can biet instance nao con song, instance nao loi de khong route request vao instance loi.
- **Stateless service**: service nen han che luu state trong memory cua tung instance. State quan trong nen nam o database, cache, message broker hoac storage dung chung.
- **Failover**: neu mot instance chet, traffic duoc chuyen sang instance con lai.
- **Observability**: log, metric, trace va health endpoint giup phat hien loi nhanh hon.

Trong bai local nay, ta mo phong HA bang 2 process `news` chay tren 2 port khac nhau. Day chua phai HA day du nhu production, nhung giup thuc tap sinh hieu request duoc route qua gateway/load balancer nhu the nao.

## 2. Chay 2 service news tren local

Yeu cau:

- JDK 21
- Maven
- Docker va Docker Compose

Khoi dong PostgreSQL va Redis truoc:

```bash
cd news
docker compose up -d postgres redis
```

Terminal 1: chay instance `news` thu nhat tren port `8081`:

```bash
cd news
SERVER_PORT=8081 mvn spring-boot:run
```

Terminal 2: chay instance `news` thu hai tren port `8082`:

```bash
cd news
SERVER_PORT=8082 mvn spring-boot:run
```

Kiem tra health:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Swagger UI cua tung instance:

- http://localhost:8081/swagger-ui.html
- http://localhost:8082/swagger-ui.html

Vi 2 instance dung chung PostgreSQL va Redis, du lieu tao o instance `8081` co the doc lai tu instance `8082`, va nguoc lai. Neu gap loi Liquibase khi start dong thoi, hay start instance thu nhat xong roi moi start instance thu hai.

Co the build jar va chay bang `java -jar`:

```bash
cd news
mvn -DskipTests package
SERVER_PORT=8081 java -jar target/news-0.0.1-SNAPSHOT.jar
SERVER_PORT=8082 java -jar target/news-0.0.1-SNAPSHOT.jar
```

## 3. Folder api-gateway

Folder `api-gateway/` duoc tao san o root repo va dang de trong. Yeu cau thuc tap sinh tu tim hieu API Gateway truoc khi code.

Can tim hieu cac y chinh:

- API Gateway la gi va khac Load Balancer nhu the nao.
- Gateway route request xuong backend service nhu the nao.
- Gateway co the xu ly cross-cutting concerns nao: authentication, authorization, rate limit, logging, timeout, retry, CORS, request/response filter.
- Cach gateway phoi hop voi health check va load balancing de tang HA.
- Khi nao nen route theo path, theo host, theo header hoac theo version API.

## 4. Bai tap tao service gateway bang Spring Cloud Gateway

Sau khi tim hieu, tao mot Spring Boot service moi trong folder `api-gateway/`.

Goi y dependency:

- `spring-cloud-starter-gateway`
- `spring-cloud-starter-loadbalancer`
- `spring-boot-starter-actuator`

Gateway chay tren port `8080`. Hai instance `news` chay tren `8081` va `8082`. Request vao gateway theo path `/api/news` hoac `/api/news/**` phai duoc route xuong `news`.

Vi du cau hinh mong doi trong `api-gateway/src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    discovery:
      client:
        simple:
          instances:
            news-service:
              - uri: http://localhost:8081
              - uri: http://localhost:8082
    gateway:
      routes:
        - id: news-service
          uri: lb://news-service
          predicates:
            - Path=/api/news,/api/news/**

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

Khi hoan thanh, thu tu chay local:

```bash
# Terminal 1
cd news
docker compose up -d postgres redis
SERVER_PORT=8081 mvn spring-boot:run

# Terminal 2
cd news
SERVER_PORT=8082 mvn spring-boot:run

# Terminal 3
cd api-gateway
mvn spring-boot:run
```

Kiem tra request qua gateway:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/news
curl -X POST http://localhost:8080/api/news \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Tin qua gateway",
    "summary": "Tao tu API Gateway",
    "content": "Noi dung chi tiet",
    "author": "intern",
    "status": "PUBLISHED"
  }'
```

Ket qua can dat:

- Gateway start thanh cong tren port `8080`.
- Route `/api/news` va `/api/news/**` xuong 2 instance `news`.
- Neu stop mot instance `news`, gateway van co the goi sang instance con lai.
- README rieng trong `api-gateway/` giai thich cach chay va cach test gateway.
