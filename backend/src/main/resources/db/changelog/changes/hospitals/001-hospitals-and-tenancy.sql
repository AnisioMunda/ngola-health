--liquibase formatted sql

-- changeset hospitalao:007-01-criar-tabela-hospitals splitStatements:true
CREATE TABLE hospitals (id UUID DEFAULT uuid_generate_v4() NOT NULL, name VARCHAR(200) NOT NULL, code VARCHAR(20) NOT NULL, type VARCHAR(50) NOT NULL, province VARCHAR(100) NOT NULL, municipality VARCHAR(100), address VARCHAR(500), phone VARCHAR(20), email VARCHAR(200), tax_id VARCHAR(50), active BOOLEAN DEFAULT TRUE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT hospitals_pkey PRIMARY KEY (id), CONSTRAINT uq_hospitals_code UNIQUE (code));
COMMENT ON COLUMN hospitals.tax_id IS 'NIF para AGT';
INSERT INTO hospitals (id, name, code, type, province, municipality, active) VALUES ('00000000-0000-0000-0000-000000000100', 'Hospital Central de Luanda', 'HCL-001', 'HOSPITAL', 'Luanda', 'Ingombota', TRUE);

--rollback DROP TABLE hospitals;

-- changeset hospitalao:007-02-adicionar-hospital-id splitStatements:false
ALTER TABLE users ADD hospital_id UUID;
ALTER TABLE patients ADD hospital_id UUID;
ALTER TABLE episodes ADD hospital_id UUID;
UPDATE users SET hospital_id = '00000000-0000-0000-0000-000000000100';
UPDATE patients SET hospital_id = '00000000-0000-0000-0000-000000000100';
UPDATE episodes SET hospital_id = '00000000-0000-0000-0000-000000000100';
ALTER TABLE users ADD CONSTRAINT fk_users_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id);
ALTER TABLE patients ADD CONSTRAINT fk_patients_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id);
ALTER TABLE episodes ADD CONSTRAINT fk_episodes_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id);

--rollback ALTER TABLE episodes DROP CONSTRAINT fk_episodes_hospital;
--rollback ALTER TABLE patients DROP CONSTRAINT fk_patients_hospital;
--rollback ALTER TABLE users DROP CONSTRAINT fk_users_hospital;
--rollback ALTER TABLE episodes DROP COLUMN hospital_id;
--rollback ALTER TABLE patients DROP COLUMN hospital_id;
--rollback ALTER TABLE users DROP COLUMN hospital_id;
