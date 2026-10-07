--liquibase formatted sql

--changeset hospitalao:024-01-active-admission-integrity
CREATE UNIQUE INDEX uq_admissions_active_bed
    ON admissions (bed_id)
    WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uq_admissions_active_patient
    ON admissions (hospital_id, patient_id)
    WHERE status = 'ACTIVE';

--rollback DROP INDEX uq_admissions_active_patient;
--rollback DROP INDEX uq_admissions_active_bed;
