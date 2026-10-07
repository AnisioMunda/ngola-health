--liquibase formatted sql

-- changeset hospitalao:004-01-criar-tabela-patients splitStatements:false
CREATE TABLE patients (id UUID DEFAULT uuid_generate_v4() NOT NULL, full_name VARCHAR(200) NOT NULL, birth_date date NOT NULL, gender GENDER_ENUM NOT NULL, national_id VARCHAR(20), health_card_number VARCHAR(30), phone VARCHAR(20), email VARCHAR(200), address VARCHAR(500), province VARCHAR(100), municipality VARCHAR(100), emergency_contact_name VARCHAR(200), emergency_contact_phone VARCHAR(20), emergency_contact_relationship VARCHAR(50), blood_type VARCHAR(5), allergies TEXT, chronic_conditions TEXT, notes TEXT, active BOOLEAN DEFAULT TRUE NOT NULL, created_by UUID, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT patients_pkey PRIMARY KEY (id), CONSTRAINT fk_patients_created_by FOREIGN KEY (created_by) REFERENCES users(id), CONSTRAINT uq_patients_national_id UNIQUE (national_id), CONSTRAINT uq_patients_health_card UNIQUE (health_card_number));
CREATE INDEX idx_patients_full_name ON patients(full_name);
CREATE INDEX idx_patients_national_id ON patients(national_id);
CREATE INDEX idx_patients_active ON patients(active);

--rollback DROP TABLE patients;
