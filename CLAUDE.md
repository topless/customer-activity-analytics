# Customer Activity Analytics — agent instructions

Web application for customer care operators at a financial-services company: search a
customer, review their card / payment / crypto activity, and run a persisted, RAG-grounded
AI risk analysis. Built for the Swissquote platform-engineering assignment.

## Layout

- `backend/` — Spring Boot 3.5 (Java 17), Maven wrapper. Package root `com.swissquote.caa`.
- `frontend/` — React 18 + TypeScript + Vite.
- `docs/` — architecture, API contract, AI methodology.
- `docker-compose.yml` — Postgres (pgvector) + backend + frontend.

## Commands

- Backend build + tests: `cd backend && ./mvnw verify`
- Backend run (needs Postgres up): `cd backend && ./mvnw spring-boot:run`
- Database only: `docker compose up -d db`
- Frontend dev server: `cd frontend && npm install && npm run dev` (proxies `/api` to :8080)
- Full stack: `docker compose up --build`

## Hard rules

- `docs/api-contract.md` is the source of truth for the HTTP API. Backend and frontend must
  both conform to it; change the contract file first, in the same commit, if an endpoint changes.
- Flyway migrations are append-only. Never edit an applied migration; add a new `V<n>__*.sql`.
- No real PII anywhere: all customers, accounts, IBANs, wallets and names are fictional, and
  seed data is generated deterministically (fixed RNG seed) by `scripts/generate_seed_data.py`.
- The application must run fully offline: the default LLM adapter is a deterministic stub.
  Real-provider adapters (Anthropic) activate only via configuration and must never be
  required for build, tests, or demo.
- Secrets (API keys, JWT signing key overrides) come from environment variables only —
  never commit them, never log them.

## Conventions

- Java: constructor injection, no field injection, no Lombok. Records for DTOs and value
  objects. One controller/service/repository per aggregate. Bean validation on request DTOs.
- Persistence: JPA entities mirror the SQL schema in `backend/src/main/resources/db/migration`;
  JSONB columns map as `String` + Jackson in the service layer; pgvector access goes through
  `JdbcTemplate` (JPA does not know the `vector` type).
- Errors: RFC 7807 `ProblemDetail` from a single `@RestControllerAdvice`.
- Frontend: TypeScript strict; API types in `src/api/types.ts` mirror the contract; no
  additional UI component libraries — hand-rolled CSS.
- Tests: unit-test services and the stub LLM/RAG pipeline without Spring where possible;
  Testcontainers-based integration tests for repositories and the API happy paths.
