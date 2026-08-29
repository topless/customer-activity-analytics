-- Core schema for Customer Activity Analytics.
-- Follows the table layout given in the assignment (transactions + per-type detail tables
-- + risk layer), extended with operators (auth), policy chunks (RAG) and ai_analyses.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE operators (
    operator_id   UUID PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    display_name  VARCHAR(200) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(30)  NOT NULL CHECK (role IN ('OPERATOR', 'SUPERVISOR')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE customers (
    customer_id     UUID PRIMARY KEY,
    customer_number VARCHAR(20)  NOT NULL UNIQUE,
    full_name       VARCHAR(200) NOT NULL,
    email           VARCHAR(200),
    country         CHAR(2)      NOT NULL,
    date_of_birth   DATE,
    kyc_level       VARCHAR(20)  NOT NULL CHECK (kyc_level IN ('BASIC', 'VERIFIED', 'ENHANCED')),
    onboarded_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE transactions (
    transaction_id UUID PRIMARY KEY,
    customer_id    UUID           NOT NULL REFERENCES customers (customer_id),
    activity_type  VARCHAR(10)    NOT NULL CHECK (activity_type IN ('CARD', 'PAYMENT', 'CRYPTO')),
    amount         NUMERIC(18, 2) NOT NULL,
    currency       VARCHAR(10)    NOT NULL,
    status         VARCHAR(20)    NOT NULL CHECK (status IN ('COMPLETED', 'PENDING', 'FAILED', 'REVERSED')),
    created_at     TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_transactions_customer_created ON transactions (customer_id, created_at DESC);

CREATE TABLE card_activity (
    transaction_id     UUID PRIMARY KEY REFERENCES transactions (transaction_id),
    card_pan           VARCHAR(20)  NOT NULL, -- masked, e.g. ****1234
    card_type          VARCHAR(20)  NOT NULL CHECK (card_type IN ('DEBIT', 'CREDIT', 'PREPAID')),
    merchant_name      VARCHAR(200) NOT NULL,
    mcc_code           VARCHAR(4)   NOT NULL,
    card_present       BOOLEAN      NOT NULL,
    authorization_code VARCHAR(20),
    decline_reason     VARCHAR(200)
);

CREATE TABLE payment_activity (
    transaction_id        UUID PRIMARY KEY REFERENCES transactions (transaction_id),
    payment_method        VARCHAR(20) NOT NULL CHECK (payment_method IN ('ACH', 'WIRE', 'SWIFT', 'P2P')),
    sender_account        VARCHAR(50) NOT NULL,
    receiver_account      VARCHAR(50) NOT NULL,
    receiver_bank_country CHAR(2)     NOT NULL
);

CREATE TABLE crypto_activity (
    transaction_id      UUID PRIMARY KEY REFERENCES transactions (transaction_id),
    blockchain          VARCHAR(20)  NOT NULL,
    wallet_address_from VARCHAR(100) NOT NULL,
    wallet_address_to   VARCHAR(100) NOT NULL,
    tx_hash             VARCHAR(100) NOT NULL,
    exchange_name       VARCHAR(100)
);

-- Risk layer

CREATE TABLE risk_rules (
    rule_id          UUID PRIMARY KEY,
    rule_name        VARCHAR(200)  NOT NULL,
    applies_to       VARCHAR(10)   NOT NULL CHECK (applies_to IN ('CARD', 'PAYMENT', 'CRYPTO', 'ALL')),
    threshold_logic  TEXT          NOT NULL,
    weight           NUMERIC(5, 2) NOT NULL
);

CREATE TABLE risk_assessments (
    assessment_id      UUID PRIMARY KEY,
    transaction_id     UUID          NOT NULL REFERENCES transactions (transaction_id),
    rule_id            UUID          NOT NULL REFERENCES risk_rules (rule_id),
    triggered_at       TIMESTAMPTZ   NOT NULL,
    score_contribution NUMERIC(5, 2) NOT NULL
);

CREATE INDEX idx_risk_assessments_tx ON risk_assessments (transaction_id);

-- RAG: policy documents ingested from backend/src/main/resources/policies at startup

CREATE TABLE policy_documents (
    document_id    UUID PRIMARY KEY,
    slug           VARCHAR(100) NOT NULL UNIQUE,
    title          VARCHAR(300) NOT NULL,
    content_sha256 VARCHAR(64)  NOT NULL,
    ingested_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE policy_chunks (
    chunk_id      UUID PRIMARY KEY,
    document_id   UUID        NOT NULL REFERENCES policy_documents (document_id) ON DELETE CASCADE,
    chunk_index   INT         NOT NULL,
    section_title VARCHAR(300),
    content       TEXT        NOT NULL,
    embedding     vector(384) NOT NULL
);

-- The corpus is small (tens of chunks); exact nearest-neighbour scan is fine, no ANN index.

-- Persisted AI analyses (spec #5)

CREATE TABLE ai_analyses (
    analysis_id          UUID PRIMARY KEY,
    customer_id          UUID         NOT NULL REFERENCES customers (customer_id),
    requested_by         UUID         NOT NULL REFERENCES operators (operator_id),
    requested_at         TIMESTAMPTZ  NOT NULL,
    completed_at         TIMESTAMPTZ,
    status               VARCHAR(20)  NOT NULL CHECK (status IN ('COMPLETED', 'FAILED')),
    model                VARCHAR(100) NOT NULL,
    risk_level           VARCHAR(20)  CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    summary              TEXT,
    findings             JSONB,
    recommendations      JSONB,
    cited_policies       JSONB,
    activity_window_from TIMESTAMPTZ,
    activity_window_to   TIMESTAMPTZ,
    transaction_count    INT,
    error_message        TEXT,
    prompt               TEXT, -- full prompt sent to the LLM, kept for auditability
    raw_response         TEXT  -- raw LLM output, kept for auditability
);

CREATE INDEX idx_ai_analyses_customer ON ai_analyses (customer_id, requested_at DESC);
