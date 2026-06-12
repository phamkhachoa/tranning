# API Gateway Training

Gateway la cua vao cua he thong. Trong bai identity nay, gateway lam 2 viec:

1. Route request tu public API path ve identity-service.
2. Verify JWT va tao cac header identity da duoc tin cay.

## 1. Chay gateway

Chay identity truoc:

```bash
cd identity-service
mvn spring-boot:run
```

Chay gateway:

```bash
cd api-gateway
mvn spring-boot:run
```

Gateway chay port `8080`, identity-service chay port `8081`.

## 2. Route hien co

| Gateway path | Service path | Ghi chu |
| --- | --- | --- |
| `POST /api/v1/auth/register` | `/auth/register` | Public |
| `POST /api/v1/auth/login` | `/auth/login` | Public |
| `POST /api/v1/auth/refresh` | `/auth/refresh` | Public |
| `GET /api/v1/users/me` | `/users/me` | Can Bearer token |
| `GET /api/admin/v1/users` | `/backoffice/users` | Can Bearer token role ADMIN |
| `POST /api/internal/v1/authz/check` | `/internal/authz/check` | Can Bearer token role ADMIN/SYSTEM |

## 3. Request flow

```text
Client
  -> API Gateway
  -> JwtAuthenticationGatewayFilter
  -> route rewrite
  -> identity-service
```

Gateway public endpoint:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@courseflow.local",
    "password": "Admin@123"
  }'
```

Gateway protected endpoint:

```bash
TOKEN="<accessToken from login>"
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```

## 4. Diem can hieu

- Client khong duoc tu gui `X-User-Id`, `X-User-Email`, `X-User-Roles`.
- Gateway xoa cac header do truoc, verify JWT, sau do moi set lai.
- Downstream service co the doc header tu gateway, nhung production phai chan direct traffic vao service.
- Secret ky JWT phai giong nhau o gateway va identity-service.
