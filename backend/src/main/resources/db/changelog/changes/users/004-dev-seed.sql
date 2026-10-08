--liquibase formatted sql

--changeset hospitalao:users-004-dev-pgcrypto context:@dev
CREATE EXTENSION IF NOT EXISTS pgcrypto;

--changeset hospitalao:users-005-dev-bootstrap-password context:@dev splitStatements:false
--validCheckSum: 1:any
DO $$
DECLARE
    initial_password TEXT;
BEGIN
    initial_password := convert_from(decode('${devAdminPasswordBase64}', 'base64'), 'UTF8');
    IF length(initial_password) < 12 THEN
        RAISE EXCEPTION 'DEV_ADMIN_INITIAL_PASSWORD_BASE64 must decode to at least 12 characters';
    END IF;
END
$$;

--changeset hospitalao:users-006-dev-bootstrap-data context:@dev
--validCheckSum: 1:any
INSERT INTO users (
    id, full_name, username, password_hash, email, register_status,
    must_change_password, hospital_id
)
VALUES (
    '00000000-0000-0000-0000-000000000900',
    'Administrador da Plataforma',
    'platform-admin',
    crypt(convert_from(decode('${devAdminPasswordBase64}', 'base64'), 'UTF8'), gen_salt('bf', 12)),
    'platform-admin@hospitalao.local',
    'ACTIVE',
    TRUE,
    NULL
)
ON CONFLICT (username) DO NOTHING;

--rollback DELETE FROM user_roles WHERE user_id = '00000000-0000-0000-0000-000000000900';
--rollback DELETE FROM users WHERE id = '00000000-0000-0000-0000-000000000900';

--changeset hospitalao:users-007-dev-bootstrap-role context:@dev
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'platform-admin'
  AND r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

--rollback DELETE FROM user_roles WHERE user_id = '00000000-0000-0000-0000-000000000900';

--changeset hospitalao:users-008-dev-demo-patient context:@dev
INSERT INTO patients (
    id, full_name, birth_date, gender, health_card_number,
    province, municipality, notes, active, hospital_id
)
VALUES (
    '00000000-0000-0000-0000-000000000901',
    'Paciente Demonstração - Dados Sintéticos',
    '1990-01-01',
    'FEMALE',
    'DEMO-000001',
    'Luanda',
    'Luanda',
    'Registo de demonstração; não contém dados de uma pessoa real.',
    TRUE,
    '00000000-0000-0000-0000-000000000100'
)
ON CONFLICT DO NOTHING;

--rollback DELETE FROM patients WHERE id = '00000000-0000-0000-0000-000000000901';
