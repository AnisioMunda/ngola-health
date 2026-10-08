--liquibase formatted sql

--changeset hospitalao:performance-001-pg-trgm
CREATE EXTENSION IF NOT EXISTS pg_trgm;
--rollback DROP EXTENSION IF EXISTS pg_trgm;

--changeset hospitalao:performance-002-patient-full-name-trgm runInTransaction:false
CREATE INDEX CONCURRENTLY idx_patients_full_name_trgm
    ON patients USING gin (lower(full_name) gin_trgm_ops);
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_patients_full_name_trgm;

--changeset hospitalao:performance-003-patient-national-id-trgm runInTransaction:false
CREATE INDEX CONCURRENTLY idx_patients_national_id_trgm
    ON patients USING gin (national_id gin_trgm_ops);
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_patients_national_id_trgm;

--changeset hospitalao:performance-004-patient-health-card-trgm runInTransaction:false
CREATE INDEX CONCURRENTLY idx_patients_health_card_trgm
    ON patients USING gin (health_card_number gin_trgm_ops);
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_patients_health_card_trgm;

--changeset hospitalao:performance-005-patient-phone-trgm runInTransaction:false
CREATE INDEX CONCURRENTLY idx_patients_phone_trgm
    ON patients USING gin (phone gin_trgm_ops);
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_patients_phone_trgm;

--changeset hospitalao:performance-006-appointment-doctor-date-start runInTransaction:false
CREATE INDEX CONCURRENTLY idx_appointments_doctor_date_start
    ON appointments (doctor_id, appointment_date, start_time);
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_appointments_doctor_date_start;

--changeset hospitalao:performance-007-episode-doctor-status-schedule runInTransaction:false
CREATE INDEX CONCURRENTLY idx_episodes_doctor_status_scheduled
    ON episodes (doctor_id, status, scheduled_at DESC NULLS LAST, created_at DESC);
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_episodes_doctor_status_scheduled;

--changeset hospitalao:performance-008-invoice-hospital-status-created runInTransaction:false
CREATE INDEX CONCURRENTLY idx_invoices_hospital_status_created
    ON invoices (hospital_id, status, created_at DESC);
--rollback DROP INDEX CONCURRENTLY IF EXISTS idx_invoices_hospital_status_created;
