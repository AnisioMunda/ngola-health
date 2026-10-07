--liquibase formatted sql

--changeset ngola:core-000-enable-uuid-ossp
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
--rollback DROP EXTENSION IF EXISTS "uuid-ossp";
