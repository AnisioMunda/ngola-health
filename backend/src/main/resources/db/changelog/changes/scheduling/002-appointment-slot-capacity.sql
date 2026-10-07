--liquibase formatted sql

-- changeset hospitalao:023-01-appointment-slot-capacity splitStatements:false
ALTER TABLE appointments ADD COLUMN slot_position INTEGER DEFAULT 1 NOT NULL;
ALTER TABLE appointments ADD CONSTRAINT ck_appointments_slot_position CHECK (slot_position > 0);
ALTER TABLE appointments DROP CONSTRAINT uq_appointment_slot;
CREATE UNIQUE INDEX uq_appointment_slot_position_active
    ON appointments (doctor_id, appointment_date, start_time, slot_position)
    WHERE status NOT IN ('CANCELLED', 'NO_SHOW');
CREATE UNIQUE INDEX uq_appointment_patient_slot_active
    ON appointments (patient_id, doctor_id, appointment_date, start_time)
    WHERE status NOT IN ('CANCELLED', 'NO_SHOW');

--rollback DROP INDEX uq_appointment_slot_position_active;
--rollback DROP INDEX uq_appointment_patient_slot_active;
--rollback ALTER TABLE appointments DROP CONSTRAINT ck_appointments_slot_position;
--rollback ALTER TABLE appointments DROP COLUMN slot_position;
--rollback ALTER TABLE appointments ADD CONSTRAINT uq_appointment_slot UNIQUE (doctor_id, appointment_date, start_time);
