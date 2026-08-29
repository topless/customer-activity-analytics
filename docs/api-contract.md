# API contract

Source of truth for the HTTP API between `frontend/` and `backend/`. All endpoints are under
`/api`, request/response bodies are JSON (camelCase). Timestamps are ISO-8601 with offset
(e.g. `2026-08-12T14:03:00Z`). Money amounts are JSON numbers with 2 decimals; they are
**not** converted between currencies.

## Authentication

Every endpoint except `POST /api/auth/login` requires `Authorization: Bearer <JWT>`.
Missing/invalid/expired token → `401` with an empty body or problem detail.

### POST /api/auth/login

Request: `{ "username": "alice", "password": "operator123" }`

`200`:
```json
{
  "token": "<jwt>",
  "expiresAt": "2026-08-29T20:00:00Z",
  "operator": { "id": "<uuid>", "username": "alice", "displayName": "Alice Meier", "role": "OPERATOR" }
}
```
`401` on bad credentials (problem detail, `detail: "Invalid username or password"`).

### GET /api/auth/me

`200`: the `operator` object above. Used to restore sessions on page reload.

## Customers

### GET /api/customers?query=&page=0&size=20

Searches by customer number (case-insensitive prefix, e.g. `CUST-10007`), full name
(case-insensitive substring) or exact customer UUID. Empty/absent `query` lists all
customers. Sorted by customer number.

`200`:
```json
{
  "content": [
    {
      "id": "<uuid>",
      "customerNumber": "CUST-10001",
      "fullName": "Anna Keller",
      "email": "anna.keller@example.ch",
      "country": "CH",
      "kycLevel": "VERIFIED",
      "riskScore": 125.5,
      "transactionCount": 87,
      "lastActivityAt": "2026-08-27T09:15:00Z"
    }
  ],
  "page": 0, "size": 20, "totalElements": 8, "totalPages": 1
}
```
`riskScore` is the sum of `risk_assessments.score_contribution` over all of the customer's
transactions (0 when none).

### GET /api/customers/{id}

`200`: customer profile:
```json
{
  "id": "<uuid>", "customerNumber": "CUST-10001", "fullName": "Anna Keller",
  "email": "anna.keller@example.ch", "country": "CH", "kycLevel": "VERIFIED",
  "dateOfBirth": "1988-04-12", "onboardedAt": "2023-06-01T08:00:00Z"
}
```
`404` if unknown id (problem detail).

### GET /api/customers/{id}/overview

Aggregated activity overview used by the dashboard header, cards and chart.

`200`:
```json
{
  "riskScore": 125.5,
  "windowFrom": "2026-03-02T10:00:00Z",
  "windowTo": "2026-08-27T09:15:00Z",
  "transactionCount": 87,
  "byType": [
    {
      "activityType": "CARD",
      "count": 60,
      "failedCount": 4,
      "totalsByCurrency": [ { "currency": "CHF", "totalAmount": 12345.67 } ]
    }
  ],
  "byStatus": [ { "status": "COMPLETED", "count": 78 } ],
  "monthlyCounts": [
    { "month": "2026-03", "card": 10, "payment": 2, "crypto": 0 }
  ],
  "triggeredRules": [
    {
      "ruleId": "<uuid>",
      "ruleName": "High-value cross-border payment",
      "appliesTo": "PAYMENT",
      "timesTriggered": 3,
      "totalContribution": 75.0,
      "lastTriggeredAt": "2026-08-20T11:00:00Z"
    }
  ]
}
```
`activityType` ∈ `CARD | PAYMENT | CRYPTO`; `byType` contains only types the customer has.
`status` ∈ `COMPLETED | PENDING | FAILED | REVERSED`. `monthlyCounts` covers every month in
the window, zero-filled, ascending. `triggeredRules` sorted by `totalContribution` desc.

### GET /api/customers/{id}/transactions?type=&status=&page=0&size=25

Optional filters `type` (activity type) and `status`. Sorted by `createdAt` desc.

`200` (same paging envelope as the search endpoint):
```json
{
  "content": [
    {
      "id": "<uuid>",
      "activityType": "PAYMENT",
      "amount": 9500.00,
      "currency": "CHF",
      "status": "COMPLETED",
      "createdAt": "2026-08-20T11:00:00Z",
      "riskScore": 55.0,
      "triggeredRules": ["High-value cross-border payment"],
      "card": null,
      "payment": {
        "paymentMethod": "SWIFT",
        "senderAccount": "CH93 0076 2011 6238 5295 7",
        "receiverAccount": "AE07 0331 2345 6789 0123 456",
        "receiverBankCountry": "AE"
      },
      "crypto": null
    }
  ],
  "page": 0, "size": 25, "totalElements": 87, "totalPages": 4
}
```
Exactly one of `card` / `payment` / `crypto` is non-null, matching `activityType`:

- `card`: `{ "cardPan": "****1234", "cardType": "CREDIT", "merchantName": "...", "mccCode": "7995", "cardPresent": false, "authorizationCode": "A1B2C3", "declineReason": null }`
- `payment`: as above
- `crypto`: `{ "blockchain": "BTC", "walletAddressFrom": "bc1q…", "walletAddressTo": "bc1q…", "txHash": "0x…", "exchangeName": "Kraken" }` (`exchangeName` nullable)

`riskScore` is the sum of that transaction's assessment contributions (0 if none).

## AI analyses

### POST /api/customers/{id}/analyses

Runs an analysis synchronously (may take a few seconds with a real LLM; the stub is
instant). No request body. `201` with the full analysis detail (below). If the LLM call
fails, the failed attempt is still persisted and returned with `status: "FAILED"` and
`errorMessage` set (still `201` — the resource was created).

### GET /api/analyses/{analysisId}

`200`:
```json
{
  "id": "<uuid>",
  "customerId": "<uuid>",
  "requestedBy": { "id": "<uuid>", "username": "alice", "displayName": "Alice Meier", "role": "OPERATOR" },
  "requestedAt": "2026-08-29T10:00:00Z",
  "completedAt": "2026-08-29T10:00:04Z",
  "status": "COMPLETED",
  "model": "stub-analyst-v1",
  "riskLevel": "HIGH",
  "summary": "…2-4 sentence narrative…",
  "findings": [
    {
      "title": "Repeated sub-threshold SWIFT payments",
      "severity": "HIGH",
      "detail": "…",
      "relatedTransactionIds": ["<uuid>"]
    }
  ],
  "recommendations": [ "Escalate to AML compliance within 24h." ],
  "citedPolicies": [
    {
      "chunkId": "<uuid>",
      "documentTitle": "AML Transaction Monitoring Policy",
      "sectionTitle": "Structuring and smurfing",
      "excerpt": "…first ~300 chars of the chunk…"
    }
  ],
  "activityWindowFrom": "2026-03-02T10:00:00Z",
  "activityWindowTo": "2026-08-27T09:15:00Z",
  "transactionCount": 87,
  "errorMessage": null
}
```
`riskLevel` ∈ `LOW | MEDIUM | HIGH | CRITICAL`. Finding `severity` ∈ `LOW | MEDIUM | HIGH`.
For `status: "FAILED"`: `riskLevel`, `summary`, `findings`, `recommendations`,
`citedPolicies` are null/empty and `errorMessage` is set.

### GET /api/customers/{id}/analyses

`200`: array (newest first) of the same shape as the detail **without** `findings`,
`recommendations`, `citedPolicies` (list view):
`[ { "id", "customerId", "requestedBy", "requestedAt", "completedAt", "status", "model", "riskLevel", "summary", "transactionCount", "errorMessage" } ]`

## Errors

Non-2xx responses use RFC 7807 problem detail, e.g.:
```json
{ "type": "about:blank", "title": "Not Found", "status": 404, "detail": "Customer 3f… not found", "instance": "/api/customers/3f…" }
```
Validation errors: `400` with `detail` describing the first violation.
