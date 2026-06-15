-- V4: performance tuning.

-- 1. Make ledger inserts batchable.
--    LedgerEntry was IDENTITY (BIGSERIAL). IDENTITY forces Hibernate to round-trip to the DB for
--    every row to read back the generated key, which DISABLES JDBC batch inserts. Switching the
--    entity to a SEQUENCE generator lets Hibernate pre-allocate ids in blocks and batch the INSERTs.
--    BIGSERIAL already created this sequence; widen its increment to match the entity's
--    allocationSize=50 so the app and DB agree on the block size (no id collisions or gaps drift).
ALTER SEQUENCE ledger_entries_id_seq INCREMENT BY 50;

-- 2. Optimize the "ledger by account, newest first" read.
--    findByAccountIdOrderByCreatedAtAsc filters on account_id AND sorts by created_at. A composite
--    index on (account_id, created_at) serves BOTH from one index (filter on the prefix, ordered
--    scan on the rest), so the old account-only index becomes redundant.
DROP INDEX idx_ledger_account;
CREATE INDEX idx_ledger_account_created ON ledger_entries(account_id, created_at);
