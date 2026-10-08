--liquibase formatted sql

-- changeset hospitalao:025-01-user-teams-id
ALTER TABLE users ADD COLUMN teams_user_id UUID;
CREATE UNIQUE INDEX uq_users_teams_user_id
    ON users(teams_user_id)
    WHERE teams_user_id IS NOT NULL;

--rollback ALTER TABLE users DROP COLUMN teams_user_id;
