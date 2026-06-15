-- V1: initial schema for users and accounts.
-- Naming: V<version>__<description>.sql — Flyway will run files in version order, exactly once each.

CREATE TABLE users (
    id          BIGSERIAL    PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id),
    currency    VARCHAR(3)      NOT NULL,                -- ISO 4217: USD, EUR, KES…
    balance     NUMERIC(19, 4)  NOT NULL DEFAULT 0,      -- money: never FLOAT/DOUBLE
    version     BIGINT          NOT NULL DEFAULT 0,      -- @Version, for optimistic locking in Phase 4
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT accounts_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
