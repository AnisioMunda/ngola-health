--liquibase formatted sql

-- changeset hospitalao:016-01-shifts splitStatements:false
CREATE TABLE shifts (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, user_id UUID NOT NULL, shift_type VARCHAR(15) DEFAULT 'MORNING' NOT NULL, shift_date date NOT NULL, start_time time WITHOUT TIME ZONE NOT NULL, end_time time WITHOUT TIME ZONE NOT NULL, department VARCHAR(100), ward_id UUID, status VARCHAR(15) DEFAULT 'SCHEDULED' NOT NULL, notes VARCHAR(300), created_by UUID, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT shifts_pkey PRIMARY KEY (id), CONSTRAINT fk_shifts_user FOREIGN KEY (user_id) REFERENCES users(id), CONSTRAINT fk_shifts_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id), CONSTRAINT fk_shifts_ward FOREIGN KEY (ward_id) REFERENCES wards(id), CONSTRAINT fk_shifts_created_by FOREIGN KEY (created_by) REFERENCES users(id));
CREATE INDEX idx_shifts_user_date ON shifts(user_id, shift_date);
CREATE INDEX idx_shifts_hospital_date ON shifts(hospital_id, shift_date);

--rollback DROP TABLE shifts;

-- changeset hospitalao:016-02-leave-requests splitStatements:false
CREATE TABLE leave_requests (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, user_id UUID NOT NULL, leave_type VARCHAR(20) NOT NULL, start_date date NOT NULL, end_date date NOT NULL, total_days INTEGER NOT NULL, reason VARCHAR(500), status VARCHAR(15) DEFAULT 'PENDING' NOT NULL, approved_by UUID, approved_at TIMESTAMP WITH TIME ZONE, rejection_reason VARCHAR(300), created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT leave_requests_pkey PRIMARY KEY (id), CONSTRAINT fk_leave_approved_by FOREIGN KEY (approved_by) REFERENCES users(id), CONSTRAINT fk_leave_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id), CONSTRAINT fk_leave_user FOREIGN KEY (user_id) REFERENCES users(id));
CREATE INDEX idx_leave_user ON leave_requests(user_id, status);
CREATE INDEX idx_leave_hospital_dates ON leave_requests(hospital_id, start_date, end_date);

--rollback DROP TABLE leave_requests;

-- changeset hospitalao:016-03-attendance splitStatements:false
CREATE TABLE attendance_records (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, user_id UUID NOT NULL, shift_id UUID, work_date date NOT NULL, check_in TIMESTAMP WITH TIME ZONE, check_out TIMESTAMP WITH TIME ZONE, minutes_worked INTEGER, overtime_minutes INTEGER DEFAULT 0, status VARCHAR(15) DEFAULT 'PRESENT' NOT NULL, notes VARCHAR(300), created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT attendance_records_pkey PRIMARY KEY (id), CONSTRAINT fk_attendance_shift FOREIGN KEY (shift_id) REFERENCES shifts(id), CONSTRAINT fk_attendance_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id), CONSTRAINT fk_attendance_user FOREIGN KEY (user_id) REFERENCES users(id));
ALTER TABLE attendance_records ADD CONSTRAINT uq_attendance_user_date UNIQUE (user_id, work_date);
CREATE INDEX idx_attendance_user_date ON attendance_records(user_id, work_date);
CREATE INDEX idx_attendance_hospital_date ON attendance_records(hospital_id, work_date);

--rollback DROP TABLE attendance_records;
