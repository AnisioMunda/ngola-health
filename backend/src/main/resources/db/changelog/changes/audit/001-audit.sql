--liquibase formatted sql

-- changeset hospitalao:015-01-audit-logs splitStatements:false
CREATE TABLE audit_logs (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID, user_id UUID, username VARCHAR(100), user_full_name VARCHAR(200), action VARCHAR(20) NOT NULL, entity_type VARCHAR(30) NOT NULL, entity_id VARCHAR(100), description VARCHAR(500) NOT NULL, old_values TEXT, new_values TEXT, ip_address VARCHAR(45), user_agent VARCHAR(500), http_method VARCHAR(10), request_url VARCHAR(500), result VARCHAR(15) DEFAULT 'SUCCESS' NOT NULL, error_message VARCHAR(500), created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT audit_logs_pkey PRIMARY KEY (id), CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id), CONSTRAINT fk_audit_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id));
CREATE INDEX idx_audit_hospital_date ON audit_logs(hospital_id, created_at);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);

--rollback DROP TABLE audit_logs;
