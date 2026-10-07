--liquibase formatted sql

-- changeset hospitalao:018-01-triage splitStatements:true
CREATE TABLE triage_records (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, patient_id UUID, queue_number INTEGER NOT NULL, patient_name_temp VARCHAR(200), patient_age_temp INTEGER, patient_gender_temp VARCHAR(10), priority VARCHAR(10) DEFAULT 'GREEN' NOT NULL, chief_complaint VARCHAR(500) NOT NULL, blood_pressure VARCHAR(20), heart_rate INTEGER, temperature DECIMAL(4, 1), oxygen_saturation INTEGER, respiratory_rate INTEGER, weight_kg DECIMAL(5, 2), pain_scale INTEGER, triage_notes TEXT, status VARCHAR(15) DEFAULT 'WAITING' NOT NULL, triaged_by UUID, episode_id UUID, triaged_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, attended_at TIMESTAMP WITH TIME ZONE, completed_at TIMESTAMP WITH TIME ZONE, CONSTRAINT triage_records_pkey PRIMARY KEY (id), CONSTRAINT fk_triage_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id), CONSTRAINT fk_triage_episode FOREIGN KEY (episode_id) REFERENCES episodes(id), CONSTRAINT fk_triage_nurse FOREIGN KEY (triaged_by) REFERENCES users(id), CONSTRAINT fk_triage_patient FOREIGN KEY (patient_id) REFERENCES patients(id));
COMMENT ON COLUMN triage_records.pain_scale IS '0-10';
CREATE SEQUENCE  IF NOT EXISTS queue_number_seq START WITH 1 INCREMENT BY 1;
CREATE INDEX idx_triage_hospital_status ON triage_records(hospital_id, status);
CREATE INDEX idx_triage_priority ON triage_records(priority, triaged_at);
CREATE INDEX idx_triage_patient ON triage_records(patient_id);

--rollback DROP TABLE triage_records;
