-- V1 baseline schema for the Khata Ledger.
-- Owned by Flyway. Hibernate is set to ddl-auto=validate.

CREATE TABLE merchants (
    id              BIGSERIAL PRIMARY KEY,
    business_name   VARCHAR(120) NOT NULL,
    phone           VARCHAR(20)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE customers (
    id              BIGSERIAL PRIMARY KEY,
    merchant_id     BIGINT NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    phone           VARCHAR(20),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- One merchant cannot have two customers with the same phone (when phone is set).
CREATE UNIQUE INDEX uq_customer_phone_per_merchant
    ON customers (merchant_id, phone)
    WHERE phone IS NOT NULL;

CREATE INDEX idx_customer_merchant ON customers (merchant_id);

CREATE TABLE transactions (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    merchant_id     BIGINT NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    -- CREDIT  = merchant gave goods on udhaar (customer's outstanding goes UP)
    -- DEBIT   = merchant received cash back  (customer's outstanding goes DOWN)
    type            VARCHAR(10)   NOT NULL CHECK (type IN ('CREDIT','DEBIT')),
    amount          NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    note            VARCHAR(255),
    occurred_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_txn_customer_occurred ON transactions (customer_id, occurred_at DESC);
CREATE INDEX idx_txn_merchant_occurred ON transactions (merchant_id, occurred_at DESC);

CREATE TABLE reminders (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    merchant_id     BIGINT NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    channel         VARCHAR(20)   NOT NULL,   -- SMS / WHATSAPP / EMAIL
    outstanding     NUMERIC(14,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING / SENT / FAILED
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ
);

CREATE INDEX idx_reminder_customer ON reminders (customer_id);
