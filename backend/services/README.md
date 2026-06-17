# Training Services

Only the CourseFlow Mini services remain in this folder.

| Service | Training Lesson | Main Skill |
| --- | --- | --- |
| `discovery-service` | Platform reference | Eureka registry; observe service discovery, do not reimplement |
| `api-gateway` | Day 1-2 | Gateway routing, Keycloak token verification, trusted headers |
| `user-management-service` | Day 3 | Profile lifecycle, validation, admin directory |
| `access-control-service` | Day 2 | RBAC, scoped roles, authorization decision |
| `course-service` | Days 4-7 | CRUD, publish workflow, learner runtime |
| `enrollment-service` | Days 6-7 | Transaction, idempotency, learner progress |
| `assignment-service` | Day 8 | Submission lifecycle, feedback, MinIO submission uploads |
| `media-service` | Platform reference | Ready-made MinIO media/video service; use from course readiness, do not reimplement |
| `chat-service` | Day 12 | Realtime WebSocket/STOMP, MongoDB history |
| `quiz-service` | Day 9 | Attempt lifecycle, scoring |
| `gradebook-service` | Day 10 | Aggregation, pass/fail, privacy |
| `notification-service` | Day 11 | Event-to-notification flow, inbox |
| `search-service` | Day 13 | Debezium CDC consumer, Elasticsearch search, suggest, recommendation-lite |
| `outbox-relay` | Day 11 | Outbox polling, retry, at-least-once delivery |

Start from the HTML roadmap in `../../training-roadmap/index.html`.
For API and method skeletons, read:

- `../../training-roadmap/api-contract.html`
- `../../training-roadmap/db-design-guide.html`
- `../../training-roadmap/service-todo.html`

Training auth is enabled by default in the gateway. Use headers such as:

```text
X-Training-User-Id: 4
X-Training-User-Email: student@courseflow.local
X-Training-User-Roles: STUDENT
```

For admin/instructor calls:

```text
X-Training-User-Id: 2
X-Training-User-Email: instructor@courseflow.local
X-Training-User-Roles: INSTRUCTOR
```
