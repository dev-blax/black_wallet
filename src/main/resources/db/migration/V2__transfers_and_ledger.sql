-- V2: money movement.
-- Two new tables:
--   transfers       — one row per logical money movement, idempotency lives here
--   ledger_entries  — audit log of every credit/debit (double-entry: each transfer = 2 rows)

CREATE TABLE transfers (
    id                      UUID            PRIMARY KEY,
    source_account_id       BIGINT          NOT NULL REFERENCES accounts(id),
    destination_account_id  BIGINT          NOT NULL REFERENCES accounts(id),
    amount                  NUMERIC(19, 4)  NOT NULL,
    idempotency_key         VARCHAR(64)     NOT NULL UNIQUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT transfers_amount_positive    CHECK (amount > 0),
    CONSTRAINT transfers_distinct_accounts  CHECK (source_account_id <> destination_account_id)
);

CREATE INDEX idx_transfers_source ON transfers(source_account_id);
CREATE INDEX idx_transfers_destination ON transfers(destination_account_id);

CREATE TABLE ledger_entries (
    id            BIGSERIAL       PRIMARY KEY,
    account_id    BIGINT          NOT NULL REFERENCES accounts(id),
    entry_type    VARCHAR(16)     NOT NULL,
    amount        NUMERIC(19, 4)  NOT NULL,
    transfer_id   UUID            REFERENCES transfers(id),  -- NULL for deposits (no counterparty)
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT ledger_entries_amount_positive CHECK (amount > 0),
    CONSTRAINT ledger_entries_type_valid CHECK (entry_type IN ('CREDIT', 'DEBIT'))
);

CREATE INDEX idx_ledger_account ON ledger_entries(account_id);
CREATE INDEX idx_ledger_transfer ON ledger_entries(transfer_id);
