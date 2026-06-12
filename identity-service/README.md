# Identity Service Training

Muc tieu folder nay la day thuc tap sinh tu xay `identity-service` tu dau. Source hien tai giu **day du dau API co ban**, nhung chi implement that **API tao user** de cac ban nhin thay luong controller -> service -> repository. Cac method service con lai van co san signature/body, nhung body chi gom huong dan implement va tam thoi tra `501 Not Implemented`.

## 1. API surface da co san

| Endpoint | Trang thai | Service method |
| --- | --- | --- |
| `POST /users` | Da implement | `UserService.createUser` |
| `POST /auth/register` | Chua implement, co huong dan trong body | `AuthService.register` |
| `POST /auth/login` | Chua implement, co huong dan trong body | `AuthService.login` |
| `POST /auth/refresh` | Chua implement, co huong dan trong body | `AuthService.refresh` |
| `GET /users/me` | Chua implement, co huong dan trong body | `UserService.currentProfile` |
| `GET /backoffice/users` | Chua implement, co huong dan trong body | `UserService.listUsers` |
| `POST /internal/authz/check` | Chua implement, co huong dan trong body | `AuthzService.check` |

### Tao user

```bash
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student1@courseflow.local",
    "password": "Student@123",
    "displayName": "Student One"
  }'
```

Ket qua mong doi:

```json
{
  "id": "...",
  "email": "student1@courseflow.local",
  "displayName": "Student One",
  "roles": ["STUDENT"]
}
```

API nay nam o:

- `controller/UserController.java`: nhan HTTP request.
- `service/UserService.java`: check email trung, hash password bang BCrypt, gan role mac dinh.
- `repository/UserRepository.java`: dung Spring Data JPA, ket noi PostgreSQL rieng cua identity-service.

Vi du endpoint chua implement:

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"student1@courseflow.local","password":"Student@123"}'
```

Ket qua hien tai se la `501 Not Implemented`. Mo `AuthService.login` de doc cac buoc can lam.

## 2. Chay service

```bash
cd identity-service
mvn spring-boot:run
```

Service mac dinh chay port `8081`.

```bash
curl http://localhost:8081/actuator/health
```

## 3. Request flow can nam

### Flow hien tai: tao user

Source hien tai chi implement that flow nay. Muc tieu la nhin ro cach request di qua cac layer co ban.

```text
Client
  -> POST /users
  -> UserController.createUser
  -> UserService.createUser
       -> check email da ton tai chua
       -> hash password bang BCrypt
       -> gan role mac dinh STUDENT
  -> UserRepository.save
       -> Spring Data JPA persist vao PostgreSQL
  -> HTTP 201 Created
```

Sau nay khi thay repository bang JPA, flow van giu nguyen y tuong:

```text
Client -> Controller -> Service -> Repository -> Database
```

Controller khong xu ly business rule. Repository khong tu quyet dinh rule tao user. Service la noi dung de dat logic nghiep vu.

### Flow sau khi co API Gateway

Gateway trong folder `../api-gateway` duoc giu san de cac ban dung khi identity-service co login/JWT.

```text
Client
  -> API Gateway : /api/v1/...
       -> JwtAuthenticationGatewayFilter
          -> neu public path: cho request di tiep
          -> neu protected path: verify Bearer token
          -> xoa header X-User-* do client gui len
          -> tao lai X-User-Id, X-User-Email, X-User-Roles tu token da verify
  -> Identity Service
  -> Controller
  -> Service
  -> Repository
```

Ly do gateway phai xoa `X-User-*`: client co the tu gia mao header. Chi header duoc gateway tao sau khi verify JWT moi dang tin.

### Flow login can tu implement

Khi thuc tap sinh implement `POST /auth/login`, request nen di nhu sau:

```text
Client
  -> POST /auth/login {email, password}
  -> AuthController.login
  -> AuthService.login
       -> UserRepository.findByEmail(email)
       -> kiem tra user ACTIVE/LOCKED/DISABLED
       -> PasswordEncoder.matches(rawPassword, user.passwordHash)
       -> load roles cua user
       -> JwtTokenService.createAccessToken(user, roles)
       -> RefreshTokenService.createRefreshToken(user)
  -> return {accessToken, refreshToken, tokenType, expiresInSeconds}
```

Chi tiet load user:

```text
email trong request
  -> UserRepository.findByEmailIgnoreCase(email)
  -> User entity
  -> UserRoleAssignmentRepository.findByUserId(user.id)
  -> RoleRepository.findAllById(roleIds)
  -> danh sach role code: ["STUDENT"], ["ADMIN"], ...
```

Khong return password hash ra response. Neu email khong ton tai hoac password sai, cung tra ve `401 Invalid credentials`, khong noi ro sai phan nao.

### Flow tao JWT access token

Sau khi login dung:

```text
User + roles
  -> tao claims:
       iss = courseflow-training-identity
       sub = user.email
       uid = user.id
       roles = role codes
       iat = now
       exp = now + 15 minutes
  -> sign bang secret HS256
  -> accessToken
```

Token la bang chung identity da xac thuc user. Identity-service tao token, gateway verify token.

### Flow request protected va verify token

Vi du sau nay co endpoint `GET /users/me`:

```text
Client
  -> GET /api/v1/users/me
     Authorization: Bearer <accessToken>

API Gateway
  -> JwtAuthenticationGatewayFilter
       -> doc Authorization header
       -> parse token
       -> verify signature bang shared secret
       -> verify issuer
       -> verify exp chua het han
       -> lay uid, sub, roles tu claims
       -> set X-User-Id, X-User-Email, X-User-Roles
  -> route den Identity Service /users/me

Identity Service
  -> JwtAuthenticationFilter hoac GatewayHeaderAuthenticationFilter
       -> doc token/header da verify
       -> tao Authentication
       -> set vao SecurityContextHolder
  -> ProfileController.me(Authentication)
  -> UserService.currentProfile(authentication.getName())
  -> UserRepository.findById(userId)
  -> return profile
```

Trong identity-service, neu tu verify JWT lan nua:

```text
Authorization header
  -> JwtAuthenticationFilter
  -> JwtTokenService.verify(token)
       -> check signature
       -> check issuer
       -> check expiration
       -> extract uid/email/roles
  -> tao UsernamePasswordAuthenticationToken
  -> SecurityContextHolder.getContext().setAuthentication(authentication)
  -> Controller co the nhan Authentication
```

Neu identity-service chi tin gateway header, production bat buoc chan direct traffic vao service bang network policy/firewall. Cho bai hoc Spring Security, nen bat dau bang cach verify JWT trong identity-service de hieu filter chain.

### Flow refresh token

Refresh token khac access token: access token ngan han va gui trong moi request; refresh token dai hon va chi dung de xin cap token moi.

```text
Client
  -> POST /auth/refresh {refreshToken}
  -> AuthController.refresh
  -> RefreshTokenService.rotate(rawRefreshToken)
       -> hash raw token
       -> RefreshTokenRepository.findByTokenHash(hash)
       -> check expiresAt
       -> check revokedAt == null
       -> tao access token moi
       -> tao refresh token moi
       -> revoke token cu
  -> return token pair moi
```

Khong luu raw refresh token trong DB:

```text
raw refresh token
  -> hash
  -> luu token_hash
```

Neu DB bi lo, attacker khong co raw refresh token de goi API.

## 4. Data model can tu thiet ke

Entity trong source chi la marker class, chua co column field. Hay tu them field va migration.

| Model | Field nen co | Ghi chu |
| --- | --- | --- |
| `User` | `id`, `email`, `passwordHash`, `displayName`, `status`, `createdAt`, `updatedAt` | Email unique, password phai la hash |
| `Role` | `id`, `code`, `name`, `description` | Vi du `ADMIN`, `INSTRUCTOR`, `STUDENT` |
| `Permission` | `id`, `code`, `description` | Vi du `user:read`, `course:publish` |
| `UserRoleAssignment` | `id`, `userId`, `roleId`, `scopeType`, `scopeId`, `createdAt` | Scope co the la PLATFORM, ORG, COURSE |
| `RolePermissionGrant` | `id`, `roleId`, `permissionId` | Noi role voi permission |
| `RefreshToken` | `id`, `userId`, `tokenHash`, `expiresAt`, `revokedAt`, `replacedByTokenId` | Luu hash, khong luu raw token |
| `SecurityAuditLog` | `id`, `actorUserId`, `action`, `ipAddress`, `createdAt` | Ghi login fail, change password, assign role |

## 5. Step-by-step implement identity-service

### Step 1: Hieu database rieng cua identity-service

Source da co `spring-boot-starter-data-jpa`, PostgreSQL driver, Liquibase va `UserRepository extends JpaRepository<User, UUID>`. Database cua identity-service chay rieng trong compose root:

```bash
cd ..
docker compose up -d identity-postgres
```

Mac dinh:

```text
JDBC URL: jdbc:postgresql://localhost:5433/identity_training
Username: identity
Password: identity123
```

Liquibase dang theo chuan CourseFlow:

```text
src/main/resources/db/changelog/db.changelog.xml
src/main/resources/db/changelog/changes/001-init.sql
```

Viec can lam tiep:

1. Mo rong migration cho cac bang: `roles`, `permissions`, `user_role_assignments`, `role_permission_grants`, `refresh_tokens`, `security_audit_logs`.
2. Moi thay doi schema moi tao them file `changes/NNN-*.sql`.
3. Dang ky file moi trong `db.changelog.xml`, giu thu tu tang dan.
4. Giu `spring.jpa.hibernate.ddl-auto=validate` de Hibernate chi validate schema, khong tu tao/sua bang.
5. Viet test tao user bi trung email.

### Step 2: Hoan thien create user

1. Them password policy: toi thieu do dai, chu hoa, chu thuong, so, ky tu dac biet.
2. Chuan hoa email ve lowercase truoc khi luu.
3. Luu `passwordHash`, khong bao gio return password/hash ra response.
4. Gan role mac dinh `STUDENT`.
5. Ghi audit log `USER_CREATED`.

### Step 3: Implement login

Endpoint can them:

```text
POST /auth/login
```

DTO goi y:

```json
{
  "email": "student1@courseflow.local",
  "password": "Student@123"
}
```

Viec can lam:

1. Tao `AuthController`.
2. Tao `AuthService.login`.
3. Tim user theo email.
4. Dung `PasswordEncoder.matches(rawPassword, passwordHash)`.
5. Reject user `LOCKED` hoac `DISABLED`.
6. Neu password sai, return `401 Unauthorized`, khong noi ro email hay password sai.
7. Neu dung, sang Step 4 de issue access token.

### Step 4: Tao JWT access token

Dependency goi y:

```xml
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.12.6</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>
```

Claim toi thieu:

| Claim | Y nghia |
| --- | --- |
| `iss` | issuer, vi du `courseflow-training-identity` |
| `sub` | email hoac username |
| `uid` | user id |
| `roles` | danh sach role code |
| `iat` | thoi diem issue |
| `exp` | thoi diem het han |

Response login goi y:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900
}
```

### Step 5: Verify JWT trong identity-service

1. Tao `JwtAuthenticationFilter extends OncePerRequestFilter`.
2. Doc header `Authorization`.
3. Verify signature, issuer va expiration.
4. Tao `Authentication` va set vao `SecurityContextHolder`.
5. Sua `SecurityConfig`: public `/auth/login`, protected `/users/me`.
6. Them endpoint `GET /users/me`.

### Step 6: Implement refresh token

Endpoint can them:

```text
POST /auth/refresh
```

Bang `refresh_tokens` nen co:

- `id`
- `user_id`
- `token_hash`
- `expires_at`
- `revoked_at`
- `replaced_by_token_id`
- `created_at`

Flow rotation:

1. Client gui raw refresh token.
2. Hash token roi tim trong DB.
3. Neu khong ton tai, het han, hoac revoked -> `401`.
4. Tao access token moi.
5. Tao refresh token moi.
6. Set `revoked_at` cho token cu va gan `replaced_by_token_id`.
7. Return cap token moi.

Nguyen tac: khong luu raw refresh token trong database.

### Step 7: Logout

Endpoint can them:

```text
POST /auth/logout
```

Viec can lam:

1. Verify access token.
2. Revoke refresh token dang dung cua user/device.
3. Neu muon revoke access token som, tao bang blacklist theo `jti`.
4. Ghi audit log `LOGOUT`.

### Step 8: Role, permission va authz

Endpoint noi bo can them:

```text
POST /internal/authz/check
```

Request goi y:

```json
{
  "userId": "...",
  "permission": "course:publish",
  "scopeType": "COURSE",
  "scopeId": "..."
}
```

Flow:

1. Lay role assignments cua user.
2. Loc theo scope.
3. Kiem tra role co grant permission khong.
4. Deny by default neu khong co rule match.

## 6. Tich hop voi API Gateway

Gateway trong folder `../api-gateway` da co san filter verify JWT. Khi identity-service implement xong login/JWT, request se di nhu sau:

```text
Client -> API Gateway /api/v1/auth/login
       -> Identity Service /auth/login
       -> access token + refresh token

Client -> API Gateway /api/v1/users/me + Authorization: Bearer ...
       -> Gateway verify JWT
       -> Gateway forward X-User-Id, X-User-Email, X-User-Roles
       -> Identity Service /users/me
```

Luu y: hien identity-service moi chi implement `POST /users`. Cac route gateway cho login/profile/authz la de thuc tap sinh dung sau khi hoan thanh cac step tren.
