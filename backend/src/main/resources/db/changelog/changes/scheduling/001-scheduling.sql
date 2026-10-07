--liquibase formatted sql

-- changeset hospitalao:012-01-doctor-schedules splitStatements:false
CREATE TABLE doctor_schedules (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, doctor_id UUID NOT NULL, day_of_week INTEGER NOT NULL, start_time time WITHOUT TIME ZONE NOT NULL, end_time time WITHOUT TIME ZONE NOT NULL, slot_duration_minutes INTEGER DEFAULT 30 NOT NULL, max_patients_per_slot INTEGER DEFAULT 1 NOT NULL, active BOOLEAN DEFAULT TRUE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT doctor_schedules_pkey PRIMARY KEY (id), CONSTRAINT fk_doctor_schedules_doctor FOREIGN KEY (doctor_id) REFERENCES users(id), CONSTRAINT fk_doctor_schedules_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id));
CREATE INDEX idx_doctor_schedules_doctor ON doctor_schedules(doctor_id);
CREATE INDEX idx_doctor_schedules_hospital_day ON doctor_schedules(hospital_id, day_of_week);

--rollback DROP TABLE doctor_schedules;

-- changeset hospitalao:012-02-schedule-blocks splitStatements:false
CREATE TABLE schedule_blocks (id UUID DEFAULT uuid_generate_v4() NOT NULL, doctor_id UUID NOT NULL, hospital_id UUID NOT NULL, block_date date NOT NULL, start_time time WITHOUT TIME ZONE, end_time time WITHOUT TIME ZONE, all_day BOOLEAN DEFAULT FALSE NOT NULL, reason VARCHAR(200), created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT schedule_blocks_pkey PRIMARY KEY (id), CONSTRAINT fk_schedule_blocks_doctor FOREIGN KEY (doctor_id) REFERENCES users(id), CONSTRAINT fk_schedule_blocks_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id));
CREATE INDEX idx_schedule_blocks_doctor_date ON schedule_blocks(doctor_id, block_date);

--rollback DROP TABLE schedule_blocks;

-- changeset hospitalao:012-03-appointments splitStatements:false
CREATE TABLE appointments (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, patient_id UUID NOT NULL, doctor_id UUID NOT NULL, episode_id UUID, appointment_date date NOT NULL, start_time time WITHOUT TIME ZONE NOT NULL, end_time time WITHOUT TIME ZONE NOT NULL, status VARCHAR(20) DEFAULT 'SCHEDULED' NOT NULL, appointment_type VARCHAR(30) DEFAULT 'OUTPATIENT' NOT NULL, reason VARCHAR(300) NOT NULL, notes TEXT, cancellation_reason VARCHAR(300), cancelled_at TIMESTAMP WITH TIME ZONE, confirmed_at TIMESTAMP WITH TIME ZONE, booked_by UUID, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT appointments_pkey PRIMARY KEY (id), CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(id), CONSTRAINT fk_appointments_booked_by FOREIGN KEY (booked_by) REFERENCES users(id), CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES users(id), CONSTRAINT fk_appointments_episode FOREIGN KEY (episode_id) REFERENCES episodes(id), CONSTRAINT fk_appointments_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id));
CREATE INDEX idx_appointments_date_doctor ON appointments(appointment_date, doctor_id);
CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_hospital_date ON appointments(hospital_id, appointment_date);
CREATE INDEX idx_appointments_status ON appointments(status);
ALTER TABLE appointments ADD CONSTRAINT uq_appointment_slot UNIQUE (doctor_id, appointment_date, start_time);

--rollback DROP TABLE appointments;
