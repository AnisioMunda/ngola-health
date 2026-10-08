--liquibase formatted sql

-- changeset hospitalao:users-003-account-lockout
ALTER TABLE users
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMP WITH TIME ZONE;

--rollback ALTER TABLE users DROP COLUMN locked_until, DROP COLUMN failed_login_attempts;
