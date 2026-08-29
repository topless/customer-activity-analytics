# Architecture

## System overview

```
 ┌──────────────────┐        ┌─────────────────────────────────────────────┐
 │  React SPA       │        │  Spring Boot backend (Java 17)              │
 │  (Vite, TS)      │  /api  │                                             │
 │  nginx in Docker ├───────►│  auth ── customers ── transactions          │
 └──────────────────┘  JWT   │              │                              │
                             │          analysis ──► LlmClient (port)      │
                             │           │   │          ├─ StubLlmClient   │
                             │           │   │          └─ AnthropicLlmClient
                             │           │   └─► PolicyRetrievalService    │
                             │           │            │  (RAG)             │
                             │           ▼            ▼                    │
                             │  ┌─────────────────────────────┐            │
                             │  │ PostgreSQL 16 + pgvector    │            │
                             │  │ activity / risk / analyses  │            │
                             │  │ policy_chunks (vector 384)  │            │
                             │  └─────────────────────────────┘            │
                             └─────────────────────────────────────────────┘
```

Three containers via docker-compose: `db` (pgvector/pgvector:pg16), `backend`, `frontend`
(nginx serving the built SPA and proxying `/api` to the backend — same-origin, so no CORS).

## Backend modules (`com.swissquote.caa`)

| Package       | Responsibility |
|---------------|----------------|
| `config`      | typed `caa.*` properties, Spring Security + JWT encoder/decoder |
| `auth`        | operator login, JWT issuance, `/api/auth/*` |
| `customer`    | customer search/profile, activity overview aggregations (SQL `GROUP BY` via `JdbcTemplate`) |
| `transaction` | JPA entities for `transactions` + the three detail tables, filtered/paged listing |
| `risk`        | `risk_rules` / `risk_assessments` entities and per-transaction rule annotations |
| `rag`         | policy ingestion (markdown → chunks → embeddings), pgvector store, retrieval |
| `analysis`    | digest building, prompt rendering, LLM port + adapters, persisted analysis runs |
| `common`      | RFC 7807 error handling, paging envelope |

Persistence is deliberately mixed: JPA/Hibernate for entities and CRUD-style access
(the assignment's preferred stack), plain `JdbcTemplate` where SQL is the clearer tool —
aggregation queries and the pgvector similarity search (JPA has no `vector` type).

## The AI analysis flow (spec #2, #4, #5)

1. `POST /api/customers/{id}/analyses` (operator identity from the JWT).
2. `DigestBuilder` assembles an `ActivityDigest`: profile facts, per-type/status aggregates,
   triggered risk rules, and the top-N transactions by risk contribution. The digest is
   **pseudonymised** — customer number only; name/email/date of birth never reach the model.
3. A retrieval query is derived from the digest (triggered rule names + activity-type
   vocabulary) and embedded; the top-k policy chunks are fetched from pgvector by cosine
   distance (RAG).
4. `PromptBuilder` renders a system prompt (role, grounding rules, strict JSON output
   schema) and a user prompt (digest + policy excerpts with chunk ids).
5. The `LlmClient` port is called. Adapters:
   - `StubLlmClient` (default, offline): a deterministic analyst that derives risk level,
     findings and recommendations from the rule signals and emits the same JSON shape a
     real model is prompted for. The full pipeline — retrieval, parsing, persistence, UI —
     is identical in stub and real mode.
   - `AnthropicLlmClient`: Anthropic Messages API, selected automatically when
     `ANTHROPIC_API_KEY` is set (`caa.llm.provider=auto|stub|anthropic`).
6. The JSON response is parsed and validated (risk level enum enforced; cited chunk ids are
   checked against what was actually retrieved — hallucinated citations are dropped;
   non-UUID transaction references are dropped).
7. The run is persisted in `ai_analyses` with risk level, summary, findings,
   recommendations, cited policy excerpts (JSONB) — plus the **full prompt and raw model
   output** for auditability. Failed runs (LLM error, invalid JSON) are persisted too and
   appear in the history: an analysis never silently disappears.

No transaction is held across the LLM call; the digest read and the final save each use
their own short transaction.

## RAG design

- Corpus: five internal policy documents (AML monitoring, card fraud, crypto risk,
  sanctions/jurisdictions, escalation/CDD) in `backend/src/main/resources/policies/`.
- Ingestion at startup: split per `## ` section (long sections split on paragraphs),
  embed, store in `policy_chunks` with a `vector(384)` column. Idempotent via content
  SHA-256 per document; changed documents are re-ingested atomically.
- Embeddings: `EmbeddingModel` port. The default `HashingEmbeddingModel` is a
  deterministic bag-of-words feature-hashing model (hashed buckets, signed, 1+log(tf)
  weights, L2-normalised). Cosine over such vectors approximates weighted token overlap —
  a sound retrieval signal for a small domain corpus, fully offline and dependency-free.
  A hosted embedding provider (e.g. Voyage) can be dropped in behind the same port, and
  the store/query side (pgvector `<=>`) stays identical.
- Retrieval: exact top-k scan (corpus is tens of chunks; an ANN index would be added at
  scale).

## Security model

- Stateless JWT (HS256, self-issued): `POST /api/auth/login` checks BCrypt hashes from the
  `operators` table and returns a signed token (8h TTL); all other endpoints require it
  via Spring Security's resource-server support. CSRF is disabled (no cookie session).
- Login hardening: an unknown username still costs one BCrypt comparison (no
  username-existence timing oracle), failures return one generic message, and an
  in-memory limiter blocks a username after 10 failed attempts in 15 minutes (HTTP 429).
- Customer search escapes LIKE wildcards, so user input matches literally.
- The signing secret is configuration (`CAA_JWT_SECRET`); the committed default is for the
  demo only.
- Data minimisation towards LLM providers as described above; prompts and raw responses
  are stored server-side for audit, never exposed via the API.

## Testing

- Unit tests (Surefire, `./mvnw test`, no Docker needed): stub analyst behaviour and risk
  banding, embedding determinism/similarity, prompt content incl. the pseudonymisation
  guarantee, analysis orchestration incl. failure paths and citation validation, auth,
  overview month zero-filling.
- Integration test (Failsafe, `./mvnw verify`, needs Docker): boots the full application
  against a Testcontainers pgvector Postgres — migrations, seed data and policy ingestion
  included — and walks the operator journey: login → search → overview → filtered
  transactions → run analysis → read persisted history; plus auth-rejection and 404 paths.
