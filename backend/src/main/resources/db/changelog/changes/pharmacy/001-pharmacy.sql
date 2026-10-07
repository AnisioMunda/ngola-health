--liquibase formatted sql

-- changeset hospitalao:009-01-criar-tabela-medications splitStatements:false
CREATE TABLE medications (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, name VARCHAR(200) NOT NULL, generic_name VARCHAR(200), dosage_form VARCHAR(50) NOT NULL, strength VARCHAR(50), unit VARCHAR(20) NOT NULL, requires_prescription BOOLEAN DEFAULT TRUE NOT NULL, min_stock_level INTEGER DEFAULT 10, active BOOLEAN DEFAULT TRUE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT medications_pkey PRIMARY KEY (id), CONSTRAINT fk_medications_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id));
CREATE INDEX idx_medications_hospital ON medications(hospital_id);
CREATE INDEX idx_medications_name ON medications(name);

--rollback DROP TABLE medications;

-- changeset hospitalao:009-02-criar-tabela-stock-batches splitStatements:false
CREATE TABLE stock_batches (id UUID DEFAULT uuid_generate_v4() NOT NULL, medication_id UUID NOT NULL, batch_number VARCHAR(50) NOT NULL, expiry_date date NOT NULL, quantity_received INTEGER NOT NULL, quantity_available INTEGER NOT NULL, unit_cost numeric(10, 2), supplier VARCHAR(200), received_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by UUID, CONSTRAINT stock_batches_pkey PRIMARY KEY (id), CONSTRAINT fk_stock_batches_created_by FOREIGN KEY (created_by) REFERENCES users(id), CONSTRAINT fk_stock_batches_medication FOREIGN KEY (medication_id) REFERENCES medications(id));
CREATE INDEX idx_stock_batches_medication ON stock_batches(medication_id);
CREATE INDEX idx_stock_batches_expiry ON stock_batches(expiry_date);

--rollback DROP TABLE stock_batches;

-- changeset hospitalao:009-03-criar-tabela-stock-movements splitStatements:false
CREATE TABLE stock_movements (id UUID DEFAULT uuid_generate_v4() NOT NULL, batch_id UUID NOT NULL, movement_type VARCHAR(20) NOT NULL, quantity INTEGER NOT NULL, patient_id UUID, episode_id UUID, reason VARCHAR(500), performed_by UUID, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT stock_movements_pkey PRIMARY KEY (id), CONSTRAINT fk_stock_movements_batch FOREIGN KEY (batch_id) REFERENCES stock_batches(id), CONSTRAINT fk_stock_movements_patient FOREIGN KEY (patient_id) REFERENCES patients(id), CONSTRAINT fk_stock_movements_episode FOREIGN KEY (episode_id) REFERENCES episodes(id), CONSTRAINT fk_stock_movements_user FOREIGN KEY (performed_by) REFERENCES users(id));
CREATE INDEX idx_stock_movements_batch ON stock_movements(batch_id);

--rollback DROP TABLE stock_movements;
