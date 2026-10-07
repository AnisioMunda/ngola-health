--liquibase formatted sql

--changeset hospitalao:users-002-super-admin-role
INSERT INTO roles (id, name, description, active)
VALUES (
    '00000000-0000-0000-0000-000000000009',
    'SUPER_ADMIN',
    'Platform administrator — manage hospitals and platform settings',
    TRUE
);
--rollback DELETE FROM roles WHERE id = '00000000-0000-0000-0000-000000000009';
