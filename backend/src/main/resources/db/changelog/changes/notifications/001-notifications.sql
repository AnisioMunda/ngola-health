--liquibase formatted sql

-- changeset hospitalao:013-01-notifications splitStatements:false
CREATE TABLE notifications (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, user_id UUID, type VARCHAR(40) NOT NULL, priority VARCHAR(10) DEFAULT 'MEDIUM' NOT NULL, title VARCHAR(200) NOT NULL, message TEXT NOT NULL, action_url VARCHAR(300), reference_id UUID, reference_type VARCHAR(30), read BOOLEAN DEFAULT FALSE NOT NULL, read_at TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT notifications_pkey PRIMARY KEY (id), CONSTRAINT fk_notifications_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id), CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id));
CREATE INDEX idx_notifications_user_read ON notifications(user_id, read);
CREATE INDEX idx_notifications_hospital ON notifications(hospital_id, created_at);
CREATE INDEX idx_notifications_type ON notifications(type);

--rollback DROP TABLE notifications;
