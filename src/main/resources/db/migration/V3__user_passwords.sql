-- V3: add password_hash to users.
-- Existing users have no password; backfill with a sentinel string that BCrypt cannot match,
-- so they are blocked from login until an admin (or a future password-reset flow) updates them.
-- The sentinel deliberately does NOT begin with "$2a$" / "$2b$", which is what valid BCrypt hashes start with.

ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(72) NOT NULL DEFAULT 'NO_PASSWORD_SET';

-- Strip the default after backfill so future inserts MUST supply a real hash.
ALTER TABLE users
    ALTER COLUMN password_hash DROP DEFAULT;
