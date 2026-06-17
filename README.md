# CourseFlow Training

Repo này là bản training được bê từ CourseFlow thật để thực tập sinh học trên source gần thực tế,
nhưng đã được thu gọn thành **CourseFlow Mini**. Mục tiêu không phải hoàn thành toàn bộ LMS
enterprise, mà là học các pattern backend phổ biến trong hầu hết hệ thống thật.

## Bắt Đầu Từ Đâu?

- [training-roadmap/index.html](training-roadmap/index.html): tổng quan chương trình 18 ngày.
- [training-roadmap/api-contract.html](training-roadmap/api-contract.html): API contract tối thiểu để web/app có surface ổn định.
- [training-roadmap/auth-flow.html](training-roadmap/auth-flow.html): luồng Authentication/Authorization đầy đủ và tài khoản demo.
- [training-roadmap/db-design-guide.html](training-roadmap/db-design-guide.html): guardrail để thực tập sinh tự thiết kế DB.
- [training-roadmap/service-todo.html](training-roadmap/service-todo.html): method TODO/skeleton theo từng service.
- [backend/TRAINING_SCOPE.md](backend/TRAINING_SCOPE.md): service giữ lại, service đã loại khỏi training.

Tìm các điểm thực hành ngay trong source:

```bash
rg 'TODO\(training' backend/services
```

Quy ước comment trong code:

- `TRAINING(...)`: contract/flow/query đã có sẵn để đọc hiểu, không mặc định là việc cần sửa.
- `TODO(training-day-XX-impl)`: điểm học viên cần implement hoặc harden, có `Step 1/2/3` ngay trong comment.

## Cách Học Trong Repo Này

Mỗi bài học có ba lớp hướng dẫn, đọc theo thứ tự này:

1. Mở HTML của ngày học trong `training-roadmap/lessons`.
2. Mở `training-roadmap/api-contract.html` để giữ đúng path cho web/app.
3. Tìm comment trong code bằng `rg 'TODO\(training' backend/services`.

Trong code:

- `TRAINING(...)`: đọc để hiểu API, flow, query, boundary đã có.
- `TODO(training-day-XX-impl)`: điểm cần implement hoặc harden trong bài đó.
- Mỗi TODO có Step 1/2/3 ngay cạnh method để biết cần sửa theo hướng nào.

Người học tự làm DB design, Liquibase migration, entity/repository query, service implementation
và manual API evidence. Automated test chưa phải trọng tâm ở phase này. Mỗi feature cần chứng minh
bằng happy path, error path và read-back query.

## Service Giữ Lại

| Service | Kỹ năng chính |
| --- | --- |
| `api-gateway` | Routing, Keycloak token verification, trusted gateway boundary |
| `user-management-service` | User/profile/directory lifecycle |
| `access-control-service` | RBAC, role assignment, permission decision |
| `course-service` | CRUD, catalog, publish workflow |
| `enrollment-service` | Transaction, idempotency, learner progress |
| `assignment-service` | Submission lifecycle, instructor feedback |
| `chat-service` | Realtime chat, WebSocket/STOMP, MongoDB message history |
| `quiz-service` | Attempt lifecycle, automatic scoring |
| `gradebook-service` | Score aggregation, pass/fail, privacy |
| `notification-service` | Notification inbox, event reaction |
| `search-service` | Elasticsearch search, autocomplete, recommendation-lite |
| `outbox-relay` | Outbox polling, retry, at-least-once delivery |
| `Prometheus/Grafana` | Observability, metrics, dashboard, alert rule |
| `Redis` | Cache/rate limit practice, not source of truth |

Web và app được giữ làm API contract/demo surface, không phải trọng tâm để thực tập sinh viết lại UI.

## Local Infra Training

```bash
cd backend/infra/docker
docker compose -f docker-compose.training.yml up -d --build
```

Tới ngày 16, bật thêm observability:

```bash
docker compose -f docker-compose.training.yml --profile observability up -d --build
```

Prometheus: `http://localhost:19090`, Grafana: `http://localhost:13000` với `admin/admin`.

Mini stack mặc định chạy Keycloak tại `http://localhost:18080` và gateway verify access token
thật bằng issuer/audience/JWKS. Trong bản training, gateway dùng `TOKEN_CONVERTER_MODE=local`:
sau khi verify token Keycloak, gateway tự sinh internal JWT ngắn hạn để downstream service nhận
`CurrentUser`.

Keycloak admin console:

```text
URL: http://localhost:18080
Username: admin
Password: admin
```

Nếu cần cứu lớp khi chưa dựng được Keycloak, bật bypass tạm thời:

```bash
COURSEFLOW_TRAINING_AUTH_BYPASS=true docker compose -f docker-compose.training.yml up -d --build
```

Khi quay lại flow Keycloak thật, recreate gateway để bỏ env bypass khỏi container cũ:

```bash
docker compose -f docker-compose.training.yml up -d --build --force-recreate api-gateway
```

Luồng training vẫn phải học đầy đủ AuthN/AuthZ:

```text
Web login/session
-> Keycloak/OIDC Authorization Code + PKCE
-> API Gateway verify external access token
-> Gateway xóa header giả mạo và set trusted headers
-> Downstream service verify internal JWT
-> CurrentUser vào service layer
-> access-control RBAC + domain resource guard
-> audit decision
```

Xem chi tiết ở [training-roadmap/auth-flow.html](training-roadmap/auth-flow.html).

## Tài Khoản Demo Training

Các tài khoản này có trong Keycloak realm import và được seed profile/RBAC trong database demo.
Mật khẩu Keycloak cho tất cả tài khoản demo là `password`.

| User ID | Email | Password | Vai trò | Dùng cho |
| --- | --- | --- | --- | --- |
| `1` | `admin@courseflow.local` | `password` | `ADMIN` | Admin web, quản trị hệ thống |
| `2` | `instructor@courseflow.local` | `password` | `INSTRUCTOR` | Admin web, course/assignment/quiz |
| `4` | `student@courseflow.local` | `password` | `STUDENT` | Learner web, enrollment/chat/quiz |
| `5` | `student2@courseflow.local` | `password` | `STUDENT` | Learner web, user phụ để demo nhiều học viên |

Đăng nhập admin web ở `http://localhost:5173/login`, learner web ở `http://localhost:3000/login`.

Tạo learner mới thuộc bài Ngày 03 - User admin và profile. Mini stack hỗ trợ hai luồng:
admin web tạo learner qua `POST /api/admin/v1/users`, hoặc learner tự đăng ký ở
`http://localhost:3000/register` qua Keycloak registration. Admin/backoffice phải dùng
`POST /api/admin/v1/users`; không dùng trang `/register` để admin tạo hộ learner. Cả hai luồng đều
phải đi qua gateway, link `issuer + subject` trong access-control và nhận role mặc định `STUDENT`.

## Quy Tắc SSO Khi Test Nhiều User

Admin web, learner web và mobile app là các OIDC client khác nhau nhưng cùng realm Keycloak
`courseflow`. Vì vậy cùng một browser profile chỉ có một SSO user đang active cho realm đó.

- Mở admin web và learner web cùng lúc với cùng user là đúng SSO.
- Muốn test `admin@courseflow.local` và `student@courseflow.local` đồng thời, dùng browser profile
  khác nhau, hoặc normal window + incognito.
- Nếu đang login admin rồi mở learner `/register`, Keycloak có thể báo
  `different_user_authenticated`. Đây là behavior đúng: registration dành cho visitor chưa có SSO
  session của user khác.
- Luồng chuẩn để admin tạo learner vẫn là admin web -> `POST /api/admin/v1/users` -> Keycloak Admin
  API -> access-control -> user-management profile.

Fallback khi bật `COURSEFLOW_TRAINING_AUTH_BYPASS=true`: mở DevTools Console và tạo session demo.

```js
localStorage.setItem("courseflow.admin.session", JSON.stringify({
  accessToken: "training",
  refreshToken: "",
  user: {
    id: 1,
    email: "admin@courseflow.local",
    fullName: "CourseFlow Admin",
    role: "ADMIN",
    status: "ACTIVE"
  }
}));
location.href = "/dashboard";
```

```js
localStorage.setItem("courseflow.admin.session", JSON.stringify({
  accessToken: "training",
  refreshToken: "",
  user: {
    id: 2,
    email: "instructor@courseflow.local",
    fullName: "Demo Instructor",
    role: "INSTRUCTOR",
    status: "ACTIVE"
  }
}));
location.href = "/dashboard";
```

Đăng nhập learner web ở `http://localhost:3000`: mở DevTools Console và chạy:

```js
localStorage.setItem("courseflow.learning.session", JSON.stringify({
  accessToken: "training",
  refreshToken: "",
  user: {
    id: 4,
    email: "student@courseflow.local",
    fullName: "Demo Learner",
    role: "STUDENT",
    status: "ACTIVE"
  }
}));
location.reload();
```

Để đổi user, sửa `id/email/fullName/role` theo bảng trên rồi reload trang.

## Build Backend

```bash
cd backend
mvn -DskipTests compile
```

## Cấu Trúc Chính

```text
backend/                  CourseFlow Mini backend services
web/react-admin/          Admin/instructor UI, giữ làm contract/demo
web/next-learning/        Learner web UI, giữ làm contract/demo
app/                      Flutter learner app, giữ làm contract/demo
training-roadmap/         HTML roadmap, API/DB/TODO docs
```

## Nguyên Tắc Hướng Dẫn

- Không đưa source rồi bỏ mặc kiểu “tự làm tiếp”.
- Không cho full DB schema ngay từ đầu.
- Không để mỗi nhóm tự nghĩ endpoint khác nhau.
- Không bắt test coverage trong phase đầu.
- Bắt buộc review DB design trước khi code.
- Bắt buộc demo được bằng API evidence.
- Bắt buộc ghi rõ AI đã giúp gì và người học đã verify lại thế nào.
