# microform

Form-based workflow system. HTML Form / JSON submission -> validation -> persistence -> state transition -> query / CSV export.

## Stack
- Spring Boot 3.5.6, Java 21
- PostgreSQL (runtime) / H2 (tests)
- Gradle Kotlin DSL
- Flyway migrations (`ddl-auto=validate`)

## Domain
- `Form` / `FormVersion` — form definition with `schema_json` and `workflow_json`
- `Submission` — state: `DRAFT -> SUBMITTED -> UNDER_REVIEW -> APPROVED|REJECTED`
- `SubmissionEvent` — append-only audit log (raw JDBC)
- `WorkflowEngine` — custom state machine driven by `workflow_json`
- `SchemaValidatorService` — type/constraint validation, no external lib

## Roles (HTTP Basic, in-memory)
| user | role |
| --- | --- |
| submitter | SUBMITTER |
| reviewer | REVIEWER |
| admin | ADMIN |

## Endpoints
- `POST /forms` — create form (admin)
- `POST /forms/{formId}/versions` — new version (admin)
- `GET /forms/{formId}` / `GET /forms/{formId}/versions/{version}`
- `POST /forms/{formId}/versions/{version}/submissions` — submit
- `GET /submissions?status=&q=&page=` — list with filter / search / pagination
- `GET /submissions/{id}` — detail with audit log
- `POST /submissions/{id}/transition` — workflow transition (role-gated)
- `GET /exports/forms/{formId}/versions/{version}.csv` — streaming CSV

## Design decisions
- Custom workflow engine (~80 LOC) instead of Spring State Machine — workflow JSON is per-form, library overhead unjustified.
- Audit log uses `JdbcTemplate` raw INSERT — bypasses JPA dirty checking, guarantees append-only.
- CSV export uses `StreamingResponseBody` + `RowCallbackHandler` + `fetchSize=500` — verified OOM-safe at `-Xmx128m` with 10k rows.
- Rate limit via Resilience4j + Caffeine — no Redis required.
- `JSONB` columns deserialized via `AttributeConverter<T, String>` to keep entity classes typed.

## Run
```bash
docker compose up -d        # postgres
./gradlew bootRun
```

## Test
```bash
./gradlew test              # H2 in-memory
```
Coverage:
- `SchemaValidatorServiceTest` — type / constraint matrix
- `WorkflowEngineTest` — transition rules, illegal transitions
- `TransitionSecurityTest` — `@WithMockUser` per role
- `CsvStreamingTest` — 10,000 rows under `-Xmx128m`

## Milestones
- [x] M1 scaffolding + form versioning
- [x] M2 submission + schema validation
- [x] M3 workflow + role-gated transitions
- [x] M4 audit log + detail view
- [x] M5 list filter / search / pagination
- [x] M6 streaming CSV export
- [x] M7 webhook + retry (HMAC-SHA256 signature, exponential backoff, dead-letter)

## Webhooks (M7)
- Subscribe via `POST /webhooks` (admin only) with `eventTypes`, `url`, optional `secret`.
- Events: `SUBMISSION_CREATED`, `SUBMISSION_TRANSITIONED`, `SUBMISSION_APPROVED`, `SUBMISSION_REJECTED`.
- Deliveries persisted in `webhook_deliveries`. Background `WebhookScheduler` polls due rows.
- Failures (transport errors or non-2xx) retried with exponential backoff (`microform.webhook.retry-base-ms`, capped at `retry-max-ms`); after `max-attempts` attempts -> `DEAD_LETTER`.
- Body signed via HMAC-SHA256 using the per-subscription secret, header `X-Microform-Signature: sha256=<hex>`.
- `GET /webhooks/{id}/deliveries`, `GET /webhooks/stats` for inspection.

## Limits / known gaps
- Auth is in-memory; production deploy needs real IdP.
- Cursor pagination not implemented; offset/limit only.

See `microform.md` and `Formwork_Project_Spec.md` for full spec.
