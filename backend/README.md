# CourseFlow Mini Backend

This backend is the training cut of CourseFlow. It keeps only the services used by the 15-day
intern roadmap. See [TRAINING_SCOPE.md](TRAINING_SCOPE.md) for the mentoring rules and removed
services.

## Module Map

```text
common-library                  Cross-cutting API/error/web helpers only
event-contracts                 Shared event schemas
services/
  discovery-service             Eureka registry for observing service discovery
  access-control-service        Product authorization, scoped roles, permissions
  user-management-service       User profiles and directory
  course-service                Course catalog, syllabus, materials metadata
  enrollment-service            Enrollment, progress, transaction/idempotency
  assignment-service            Assignments, submissions, instructor feedback
  media-service                 Ready-made MinIO-backed media/video service
  chat-service                  Realtime course chat, WebSocket/STOMP, MongoDB
  notification-service          Inbox, preferences, realtime push
  search-service                Debezium CDC consumer and Elasticsearch search/read model
  gradebook-service             Grade items, weights, final grades, rubric audit
  quiz-service                  Quizzes, attempts, auto-grading
  api-gateway                   Edge routing, Keycloak token verification, CORS
  outbox-relay                  Outbox polling and event publishing
infra/
  docker                        Local infra and docker-compose.training.yml
```

## Local Infra

```bash
cd infra/docker
docker compose -f docker-compose.training.yml up -d --build
```

Postgres creates only the databases used by CourseFlow Mini. Service schema and demo data are owned
by each service's Liquibase changelog.

The training compose starts Keycloak at `http://localhost:18080`, imports
`infra/docker/keycloak/courseflow-realm.json`, and configures the gateway to verify Keycloak access
tokens. To keep the auth lesson smaller than production, `TOKEN_CONVERTER_MODE=local` makes the
gateway mint the short-lived internal JWT locally after OIDC verification.

The compose file also starts platform services that interns use but do not reimplement in the first
15 days:

| Service | URL | Purpose |
| --- | --- | --- |
| Eureka discovery | `http://localhost:8761` | Observe registered services and discovery concepts. |
| MinIO API | `http://localhost:9000` | S3-compatible object storage used by media and assignment uploads. |
| MinIO console | `http://localhost:9001` | Inspect buckets `courseflow-media` and `courseflow-submissions`. Login `courseflow/courseflow`. |
| Kafka Connect | `http://localhost:18083` | Runs Debezium connector `courseflow-course-search-cdc` for Day 13 search indexing. |
| media-service | via gateway `/api/v1/media/**` | Signed upload/download URLs, video metadata and readiness checks. |

Day 13 uses the CDC path for search:

```text
cf_course.public.courses
  -> Debezium PostgreSQL connector
  -> Kafka topic courseflow.course.public.courses
  -> search-service consumer
  -> Elasticsearch index courseflow-course-search
```

Keycloak admin console:

```text
URL: http://localhost:18080
Username: admin
Password: admin
```

Training bypass is only the fast fallback mode. Use
`COURSEFLOW_TRAINING_AUTH_BYPASS=true` when a learner needs to continue without Keycloak. The
security lesson must still cover the full flow: browser login with OIDC/PKCE, Keycloak access token,
gateway JWT verification, trusted internal identity propagation, access-control RBAC checks and
domain resource guards. See
[`../training-roadmap/auth-flow.html`](../training-roadmap/auth-flow.html).

Demo users seeded in Keycloak and Liquibase:

| User ID | Email | Password | Role | Use |
| --- | --- | --- | --- | --- |
| `1` | `admin@courseflow.local` | `password` | `ADMIN` | Admin web and platform operations |
| `2` | `instructor@courseflow.local` | `password` | `INSTRUCTOR` | Course authoring and assessment |
| `4` | `student@courseflow.local` | `password` | `STUDENT` | Learner web and course runtime |
| `5` | `student2@courseflow.local` | `password` | `STUDENT` | Second learner for privacy/denied cases |

## Build

```bash
mvn -DskipTests compile
```

Automated tests are optional in the first training pass. For each feature, require manual API
evidence first: happy path, error path and read-back query.

## Service Rule

No service may query another service's database. Cross-service data access must use API calls, events, or dedicated read models.
