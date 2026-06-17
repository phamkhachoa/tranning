# CourseFlow Mini Backend Scope

This training repository intentionally keeps only the services needed for the 18-day training roadmap.
The goal is to teach common backend patterns without asking learners to understand the full enterprise
CourseFlow surface on day one.

## Keep In Scope

| Module | Why It Stays |
| --- | --- |
| `common-library` | Shared error, current-user and internal JWT helpers. |
| `event-contracts` | Small event contract surface for outbox/notification learning. |
| `services/discovery-service` | Eureka registry for observing service discovery. Keep it as platform infrastructure, not a learner implementation task. |
| `services/api-gateway` | Edge routing, training auth headers and trusted gateway propagation. |
| `services/user-management-service` | User/profile/directory lifecycle. |
| `services/access-control-service` | RBAC, permission decision and authorization audit. |
| `services/course-service` | CRUD, catalog, authoring and publish workflow. |
| `services/enrollment-service` | Transaction, idempotency and learner progress. |
| `services/assignment-service` | Submission lifecycle and instructor feedback. |
| `services/media-service` | Media metadata, signed URLs, MinIO-backed upload/download and video readiness. Keep it as a ready-made platform service. |
| `services/chat-service` | Realtime course chat, WebSocket/STOMP and MongoDB message history. |
| `services/quiz-service` | Attempt lifecycle and automatic scoring. |
| `services/gradebook-service` | Score aggregation and learner privacy. |
| `services/notification-service` | Notification inbox and event reaction. |
| `services/search-service` | Debezium CDC consumer, Elasticsearch search, autocomplete and recommendation-lite without SQL `LIKE`. |
| `services/outbox-relay` | Async event relay, retry and dead-letter concepts. |
| `infra/observability` | Prometheus/Grafana training profile for metrics, scrape targets, dashboards and alert rules. |
| `redis` | Cache/rate-limit practice. Redis must not become the source of truth for business invariants. |

## Removed From Training Scope

These services are valuable in the real product, but distract from the first training pass:

- analytics
- announcement
- certificate
- deadline
- discussion
- identity-token-converter
- live-session
- loyalty
- organization
- peer-review
- portfolio
- promotion
- review

Add one back only as an explicit advanced assignment after the core flow is working.

## Training Mode

Use the hybrid method:

1. Give the user story, API path and acceptance criteria.
2. The learner proposes DB design and migration.
3. Review DB constraints and service boundary.
4. The learner implements service/repository/controller changes.
5. The learner proves the feature with manual API evidence.

Automated tests are optional in the first pass. Keep a list of tests that should be written in the
next phase, but do not block the first 18 days on test coverage.

`media-service`, `discovery-service` and observability are exceptions to the normal implementation rule. They stay
in the training stack because media storage and service discovery are important platform concepts,
but learners should not spend the first 18 days reimplementing them. Use them as reference
services while implementing course, assignment, search, chat, assessment and platform hardening flows.

Debezium/Kafka Connect is also included as platform infrastructure for Day 13. Learners should
configure and observe the connector, understand the CDC envelope, and implement/search-service
projection behavior. They should not build a custom database polling loop.

Prometheus/Grafana is included as optional profile infrastructure for Day 16. Learners should scrape
existing actuator metrics, add small domain metrics only when useful, and avoid high-cardinality
labels such as user id, email, token, raw query string or full URL.
