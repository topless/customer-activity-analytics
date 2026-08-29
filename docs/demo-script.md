# Demo script (10–15 minutes)

A suggested walkthrough for presenting the application.

## 0. Setup (before the demo)

`docker compose up --build`, wait for healthy, open http://localhost:3000.

## 1. Login & operators (1 min) — spec #3

- Log in as `alice / operator123`. Point out the operator name + role in the header.
- Mention: BCrypt-hashed operators in the DB, stateless HS256 JWT, every endpoint guarded;
  a second operator (`bob`, supervisor) will appear later in the analysis audit trail.

## 2. Customer search (1 min) — spec #1

- The landing page lists all seeded customers with live risk scores.
- Search `weber` (name) and `CUST-10005` (customer number) — both work, as does a raw UUID.

## 3. Activity dashboard (3 min) — spec #1, #2

Open **CUST-10004 · Lukas Weber** (risk 565):

- Overview cards: 50 transactions across the three activity types, volumes per currency.
- Monthly activity chart: card/payment/crypto mix over six months.
- Triggered-rules panel: structuring, unhosted-wallet transfers, high-value cross-border,
  a payment to a FATF-listed jurisdiction — this is the deterministic rule layer from the
  `risk_assessments` / `risk_rules` tables.
- Transactions table: filter to PAYMENT, expand the 27'300 CHF SWIFT to AE (accounts,
  receiver country, rule annotation); filter status FAILED to show the blocked 25'000 CHF
  attempt to MM; switch to CRYPTO and expand a transfer to the reused unhosted wallet
  (no exchange attribution).

## 4. AI analysis (4 min) — spec #2, #4, #5

- Click **Run AI analysis**. Explain what happens while it runs: pseudonymised activity
  digest → policy-chunk retrieval from pgvector (RAG) → prompt → LLM port → validated
  JSON → persisted.
- Walk the result: CRITICAL banner, narrative summary, findings each linked to concrete
  transactions, recommendations (24h AML escalation, MROS case file), **cited policy
  excerpts** — the RAG grounding (spec #4).
- Run analyses on **Elena (HIGH — gambling + declines)** and **Anna (LOW — routine)** to
  show the model differentiates.
- Log out, log in as `bob / supervisor123`, run one more analysis on Lukas, then show the
  **history panel**: all runs persisted with requester, timestamp, model and outcome
  (spec #5). Mention failed runs are persisted and shown too.

## 5. Under the hood (3 min)

- `docs/api-contract.md` — contract-first development; it's what allowed the frontend to
  be built by a parallel AI agent.
- `scripts/generate_seed_data.py` — deterministic archetypes + rule engine.
- `StubLlmClient` vs `AnthropicLlmClient` — same JSON contract; flip to a real model with
  one environment variable (show `ANTHROPIC_API_KEY=... docker compose up` if a key is at
  hand).
- Tests: `./mvnw test` (21 unit tests, incl. the prompt-pseudonymisation guarantee) and
  the Testcontainers journey IT.

## 6. AI-built methodology (2 min) — assignment focus

- `CLAUDE.md` + `docs/ai-methodology.md`: standing agent instructions, contract-first
  workflow, subagent parallelism, adversarial multi-agent review before delivery.
- Git history tells the story commit by commit.
