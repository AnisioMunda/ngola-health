--liquibase formatted sql

-- changeset hospitalao:020-01-telemedicine splitStatements:false
CREATE TABLE telemedicine_sessions (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, appointment_id UUID, patient_id UUID NOT NULL, doctor_id UUID NOT NULL, status VARCHAR(15) DEFAULT 'SCHEDULED' NOT NULL, room_token VARCHAR(255) NOT NULL, scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL, started_at TIMESTAMP WITH TIME ZONE, ended_at TIMESTAMP WITH TIME ZONE, duration_minutes INTEGER, clinical_notes TEXT, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT telemedicine_sessions_pkey PRIMARY KEY (id), CONSTRAINT fk_tele_patient FOREIGN KEY (patient_id) REFERENCES patients(id), CONSTRAINT fk_tele_doctor FOREIGN KEY (doctor_id) REFERENCES users(id), CONSTRAINT fk_tele_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id), CONSTRAINT fk_tele_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id), UNIQUE (room_token));
CREATE INDEX idx_tele_hospital_status ON telemedicine_sessions(hospital_id, status);
CREATE INDEX idx_tele_patient ON telemedicine_sessions(patient_id);
CREATE INDEX idx_tele_doctor_date ON telemedicine_sessions(doctor_id, scheduled_at);
CREATE INDEX idx_tele_room_token ON telemedicine_sessions(room_token);

--rollback DROP TABLE telemedicine_sessions;
