--liquibase formatted sql

-- changeset hospitalao:008-01-criar-tabela-lab-tests splitStatements:false
CREATE TABLE lab_tests (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, code VARCHAR(30) NOT NULL, name VARCHAR(200) NOT NULL, category VARCHAR(50) NOT NULL, sample_type VARCHAR(50), turnaround_hours INTEGER DEFAULT 24, price numeric(10, 2), reference_values TEXT, active BOOLEAN DEFAULT TRUE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT lab_tests_pkey PRIMARY KEY (id), CONSTRAINT fk_lab_tests_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id));
ALTER TABLE lab_tests ADD CONSTRAINT uq_lab_tests_hospital_code UNIQUE (hospital_id, code);
CREATE INDEX idx_lab_tests_hospital ON lab_tests(hospital_id);

--rollback DROP TABLE lab_tests;

-- changeset hospitalao:008-02-criar-tabela-lab-requests splitStatements:false
CREATE TABLE lab_requests (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, episode_id UUID, patient_id UUID NOT NULL, requested_by UUID, status VARCHAR(20) DEFAULT 'PENDING' NOT NULL, priority VARCHAR(10) DEFAULT 'NORMAL' NOT NULL, clinical_notes TEXT, collected_at TIMESTAMP WITH TIME ZONE, completed_at TIMESTAMP WITH TIME ZONE, created_by UUID, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT lab_requests_pkey PRIMARY KEY (id), CONSTRAINT fk_lab_requests_created_by FOREIGN KEY (created_by) REFERENCES users(id), CONSTRAINT fk_lab_requests_episode FOREIGN KEY (episode_id) REFERENCES episodes(id), CONSTRAINT fk_lab_requests_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id), CONSTRAINT fk_lab_requests_doctor FOREIGN KEY (requested_by) REFERENCES users(id), CONSTRAINT fk_lab_requests_patient FOREIGN KEY (patient_id) REFERENCES patients(id));
CREATE INDEX idx_lab_requests_patient ON lab_requests(patient_id);
CREATE INDEX idx_lab_requests_hospital ON lab_requests(hospital_id);
CREATE INDEX idx_lab_requests_status ON lab_requests(status);

--rollback DROP TABLE lab_requests;

-- changeset hospitalao:008-03-criar-tabela-lab-request-items splitStatements:false
CREATE TABLE lab_request_items (id UUID DEFAULT uuid_generate_v4() NOT NULL, request_id UUID NOT NULL, lab_test_id UUID NOT NULL, result_value TEXT, result_unit VARCHAR(30), reference_range VARCHAR(100), is_abnormal BOOLEAN DEFAULT FALSE, result_notes TEXT, resulted_at TIMESTAMP WITH TIME ZONE, resulted_by UUID, CONSTRAINT lab_request_items_pkey PRIMARY KEY (id), CONSTRAINT fk_lab_items_technician FOREIGN KEY (resulted_by) REFERENCES users(id), CONSTRAINT fk_lab_items_test FOREIGN KEY (lab_test_id) REFERENCES lab_tests(id), CONSTRAINT fk_lab_items_request FOREIGN KEY (request_id) REFERENCES lab_requests(id));
CREATE INDEX idx_lab_items_request ON lab_request_items(request_id);

--rollback DROP TABLE lab_request_items;
