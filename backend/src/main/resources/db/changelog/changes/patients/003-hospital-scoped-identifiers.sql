--liquibase formatted sql

-- changeset hospitalao:022-01-patient-identificadores-por-hospital
ALTER TABLE patients DROP CONSTRAINT uq_patients_national_id;
ALTER TABLE patients DROP CONSTRAINT uq_patients_health_card;
DROP INDEX idx_patients_national_id;

ALTER TABLE patients
    ADD CONSTRAINT uq_patients_hospital_national_id UNIQUE (hospital_id, national_id);
ALTER TABLE patients
    ADD CONSTRAINT uq_patients_hospital_health_card UNIQUE (hospital_id, health_card_number);

CREATE INDEX idx_patients_hospital_active_name ON patients (hospital_id, active, full_name);

-- rollback exige identificadores globalmente únicos; pode falhar se hospitais distintos tiverem reutilizado o mesmo número.
--rollback DROP INDEX idx_patients_hospital_active_name;
--rollback ALTER TABLE patients DROP CONSTRAINT uq_patients_hospital_health_card;
--rollback ALTER TABLE patients DROP CONSTRAINT uq_patients_hospital_national_id;
--rollback ALTER TABLE patients ADD CONSTRAINT uq_patients_health_card UNIQUE (health_card_number);
--rollback ALTER TABLE patients ADD CONSTRAINT uq_patients_national_id UNIQUE (national_id);
--rollback CREATE INDEX idx_patients_national_id ON patients (national_id);
