--liquibase formatted sql

-- changeset hospitalao:014-01-wards splitStatements:false
CREATE TABLE wards (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, name VARCHAR(100) NOT NULL, code VARCHAR(20) NOT NULL, type VARCHAR(20) DEFAULT 'GENERAL' NOT NULL, floor VARCHAR(10), total_beds INTEGER DEFAULT 0 NOT NULL, responsible_doctor_id UUID, notes TEXT, active BOOLEAN DEFAULT TRUE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT wards_pkey PRIMARY KEY (id), CONSTRAINT fk_wards_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id), CONSTRAINT fk_wards_doctor FOREIGN KEY (responsible_doctor_id) REFERENCES users(id));
ALTER TABLE wards ADD CONSTRAINT uq_wards_hospital_code UNIQUE (hospital_id, code);
CREATE INDEX idx_wards_hospital ON wards(hospital_id);

--rollback DROP TABLE wards;

-- changeset hospitalao:014-02-beds splitStatements:false
CREATE TABLE beds (id UUID DEFAULT uuid_generate_v4() NOT NULL, ward_id UUID NOT NULL, hospital_id UUID NOT NULL, bed_number VARCHAR(10) NOT NULL, status VARCHAR(15) DEFAULT 'AVAILABLE' NOT NULL, type VARCHAR(15) DEFAULT 'STANDARD' NOT NULL, notes VARCHAR(300), active BOOLEAN DEFAULT TRUE NOT NULL, CONSTRAINT beds_pkey PRIMARY KEY (id), CONSTRAINT fk_beds_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id), CONSTRAINT fk_beds_ward FOREIGN KEY (ward_id) REFERENCES wards(id));
ALTER TABLE beds ADD CONSTRAINT uq_bed_ward_number UNIQUE (ward_id, bed_number);
CREATE INDEX idx_beds_ward_status ON beds(ward_id, status);

--rollback DROP TABLE beds;

-- changeset hospitalao:014-03-admissions splitStatements:true
CREATE TABLE admissions (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, patient_id UUID NOT NULL, episode_id UUID, bed_id UUID NOT NULL, ward_id UUID NOT NULL, responsible_doctor_id UUID NOT NULL, status VARCHAR(15) DEFAULT 'ACTIVE' NOT NULL, admission_date TIMESTAMP WITH TIME ZONE NOT NULL, expected_discharge_date date, discharge_date TIMESTAMP WITH TIME ZONE, admission_reason VARCHAR(500) NOT NULL, diagnosis TEXT, discharge_notes TEXT, discharge_condition VARCHAR(30), admitted_by UUID, discharged_by UUID, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT admissions_pkey PRIMARY KEY (id), CONSTRAINT fk_admissions_discharged_by FOREIGN KEY (discharged_by) REFERENCES users(id), CONSTRAINT fk_admissions_admitted_by FOREIGN KEY (admitted_by) REFERENCES users(id), CONSTRAINT fk_admissions_episode FOREIGN KEY (episode_id) REFERENCES episodes(id), CONSTRAINT fk_admissions_bed FOREIGN KEY (bed_id) REFERENCES beds(id), CONSTRAINT fk_admissions_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id), CONSTRAINT fk_admissions_doctor FOREIGN KEY (responsible_doctor_id) REFERENCES users(id), CONSTRAINT fk_admissions_ward FOREIGN KEY (ward_id) REFERENCES wards(id), CONSTRAINT fk_admissions_patient FOREIGN KEY (patient_id) REFERENCES patients(id));
COMMENT ON COLUMN admissions.discharge_condition IS 'IMPROVED, STABLE, CRITICAL, DECEASED';
CREATE INDEX idx_admissions_patient ON admissions(patient_id);
CREATE INDEX idx_admissions_bed ON admissions(bed_id);
CREATE INDEX idx_admissions_status ON admissions(status);
CREATE INDEX idx_admissions_hospital_status ON admissions(hospital_id, status);

--rollback DROP TABLE admissions;

-- changeset hospitalao:014-04-transfers splitStatements:false
CREATE TABLE bed_transfers (id UUID DEFAULT uuid_generate_v4() NOT NULL, admission_id UUID NOT NULL, from_bed_id UUID NOT NULL, to_bed_id UUID NOT NULL, from_ward_id UUID NOT NULL, to_ward_id UUID NOT NULL, reason VARCHAR(300), transferred_by UUID, transferred_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT bed_transfers_pkey PRIMARY KEY (id), CONSTRAINT fk_transfers_from_ward FOREIGN KEY (from_ward_id) REFERENCES wards(id), CONSTRAINT fk_transfers_admission FOREIGN KEY (admission_id) REFERENCES admissions(id), CONSTRAINT fk_transfers_to_ward FOREIGN KEY (to_ward_id) REFERENCES wards(id), CONSTRAINT fk_transfers_to_bed FOREIGN KEY (to_bed_id) REFERENCES beds(id), CONSTRAINT fk_transfers_from_bed FOREIGN KEY (from_bed_id) REFERENCES beds(id), CONSTRAINT fk_transfers_user FOREIGN KEY (transferred_by) REFERENCES users(id));
CREATE INDEX idx_transfers_admission ON bed_transfers(admission_id);

--rollback DROP TABLE bed_transfers;
