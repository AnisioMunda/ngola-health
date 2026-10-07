--liquibase formatted sql

-- changeset hospitalao:017-01-prescriptions splitStatements:false
CREATE TABLE prescriptions (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, patient_id UUID NOT NULL, episode_id UUID, admission_id UUID, doctor_id UUID NOT NULL, status VARCHAR(25) DEFAULT 'ACTIVE' NOT NULL, prescription_date date NOT NULL, expiry_date date NOT NULL, diagnosis VARCHAR(500), notes TEXT, prescription_number VARCHAR(30) NOT NULL, cancelled_reason VARCHAR(300), cancelled_by UUID, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT prescriptions_pkey PRIMARY KEY (id), CONSTRAINT fk_prescriptions_patient FOREIGN KEY (patient_id) REFERENCES patients(id), CONSTRAINT fk_prescriptions_doctor FOREIGN KEY (doctor_id) REFERENCES users(id), CONSTRAINT fk_prescriptions_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users(id), CONSTRAINT fk_prescriptions_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id), CONSTRAINT fk_prescriptions_episode FOREIGN KEY (episode_id) REFERENCES episodes(id), CONSTRAINT fk_prescriptions_admission FOREIGN KEY (admission_id) REFERENCES admissions(id), UNIQUE (prescription_number));
CREATE SEQUENCE  IF NOT EXISTS prescription_number_seq START WITH 1 INCREMENT BY 1;
CREATE INDEX idx_prescriptions_patient ON prescriptions(patient_id);
CREATE INDEX idx_prescriptions_hospital_date ON prescriptions(hospital_id, prescription_date);
CREATE INDEX idx_prescriptions_episode ON prescriptions(episode_id);
CREATE INDEX idx_prescriptions_status ON prescriptions(status);

--rollback DROP TABLE prescriptions;

-- changeset hospitalao:017-02-prescription-items splitStatements:false
CREATE TABLE prescription_items (id UUID DEFAULT uuid_generate_v4() NOT NULL, prescription_id UUID NOT NULL, medication_id UUID NOT NULL, quantity_prescribed INTEGER NOT NULL, quantity_dispensed INTEGER DEFAULT 0 NOT NULL, dosage VARCHAR(200) NOT NULL, frequency_hours INTEGER, duration_days INTEGER, route VARCHAR(50), instructions VARCHAR(300), status VARCHAR(15) DEFAULT 'PENDING' NOT NULL, CONSTRAINT prescription_items_pkey PRIMARY KEY (id), CONSTRAINT fk_presc_items_medication FOREIGN KEY (medication_id) REFERENCES medications(id), CONSTRAINT fk_presc_items_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(id));
CREATE INDEX idx_presc_items_prescription ON prescription_items(prescription_id);
CREATE INDEX idx_presc_items_medication ON prescription_items(medication_id);

--rollback DROP TABLE prescription_items;

-- changeset hospitalao:017-03-dispensations splitStatements:false
CREATE TABLE dispensations (id UUID DEFAULT uuid_generate_v4() NOT NULL, prescription_item_id UUID NOT NULL, medication_id UUID NOT NULL, stock_batch_id UUID NOT NULL, dispensed_by UUID NOT NULL, quantity_dispensed INTEGER NOT NULL, dispensed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, notes VARCHAR(300), CONSTRAINT dispensations_pkey PRIMARY KEY (id), CONSTRAINT fk_dispensations_item FOREIGN KEY (prescription_item_id) REFERENCES prescription_items(id), CONSTRAINT fk_dispensations_user FOREIGN KEY (dispensed_by) REFERENCES users(id), CONSTRAINT fk_dispensations_medication FOREIGN KEY (medication_id) REFERENCES medications(id), CONSTRAINT fk_dispensations_batch FOREIGN KEY (stock_batch_id) REFERENCES stock_batches(id));
CREATE INDEX idx_dispensations_item ON dispensations(prescription_item_id);
CREATE INDEX idx_dispensations_batch ON dispensations(stock_batch_id);

--rollback DROP TABLE dispensations;
