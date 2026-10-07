--liquibase formatted sql

-- changeset hospitalao:021-01-equipment splitStatements:false
CREATE TABLE equipment (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, name VARCHAR(200) NOT NULL, code VARCHAR(50) NOT NULL, brand VARCHAR(100), model VARCHAR(100), serial_number VARCHAR(100), category VARCHAR(20) DEFAULT 'OTHER' NOT NULL, location VARCHAR(200), ward_id UUID, status VARCHAR(15) DEFAULT 'ACTIVE' NOT NULL, purchase_date date, purchase_price DECIMAL(12, 2), warranty_expiry date, next_maintenance_date date, maintenance_interval_days INTEGER DEFAULT 365, next_calibration_date date, notes TEXT, active BOOLEAN DEFAULT TRUE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT equipment_pkey PRIMARY KEY (id), CONSTRAINT fk_equip_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id), CONSTRAINT fk_equip_ward FOREIGN KEY (ward_id) REFERENCES wards(id), UNIQUE (code));
CREATE INDEX idx_equipment_hospital ON equipment(hospital_id, status);
CREATE INDEX idx_equipment_maintenance ON equipment(hospital_id, next_maintenance_date);

--rollback DROP TABLE equipment;

-- changeset hospitalao:021-02-maintenance splitStatements:false
CREATE TABLE maintenance_records (id UUID DEFAULT uuid_generate_v4() NOT NULL, equipment_id UUID NOT NULL, type VARCHAR(15) NOT NULL, performed_by UUID, performed_at TIMESTAMP WITH TIME ZONE NOT NULL, description TEXT NOT NULL, cost DECIMAL(10, 2), next_maintenance_date date, parts_replaced VARCHAR(500), result VARCHAR(20) DEFAULT 'OK', created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT maintenance_records_pkey PRIMARY KEY (id), CONSTRAINT fk_maint_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id), CONSTRAINT fk_maint_user FOREIGN KEY (performed_by) REFERENCES users(id));
CREATE INDEX idx_maint_equipment ON maintenance_records(equipment_id, performed_at);

--rollback DROP TABLE maintenance_records;
