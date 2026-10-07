--liquibase formatted sql

-- changeset hospitalao:019-01-patient-portal splitStatements:false
CREATE TABLE patient_portal_accounts (id UUID DEFAULT uuid_generate_v4() NOT NULL, patient_id UUID NOT NULL, email VARCHAR(200) NOT NULL, password_hash VARCHAR(255) NOT NULL, active BOOLEAN DEFAULT TRUE NOT NULL, email_verified BOOLEAN DEFAULT FALSE NOT NULL, last_login_at TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT patient_portal_accounts_pkey PRIMARY KEY (id), CONSTRAINT fk_portal_patient FOREIGN KEY (patient_id) REFERENCES patients(id), UNIQUE (patient_id), UNIQUE (email));
CREATE INDEX idx_portal_accounts_email ON patient_portal_accounts(email);
CREATE INDEX idx_portal_accounts_patient ON patient_portal_accounts(patient_id);

--rollback DROP TABLE patient_portal_accounts;

-- changeset hospitalao:019-02-portal-tokens splitStatements:false
CREATE TABLE patient_portal_tokens (id UUID DEFAULT uuid_generate_v4() NOT NULL, account_id UUID NOT NULL, token_type VARCHAR(20) NOT NULL, token_hash VARCHAR(255) NOT NULL, expires_at TIMESTAMP WITH TIME ZONE NOT NULL, used_at TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT patient_portal_tokens_pkey PRIMARY KEY (id), CONSTRAINT fk_portal_token_account FOREIGN KEY (account_id) REFERENCES patient_portal_accounts(id));

--rollback DROP TABLE patient_portal_tokens;

-- changeset hospitalao:019-03-portal-notifications splitStatements:false
CREATE TABLE patient_notifications (id UUID DEFAULT uuid_generate_v4() NOT NULL, patient_id UUID NOT NULL, channel VARCHAR(10) NOT NULL, type VARCHAR(30) NOT NULL, subject VARCHAR(200), body TEXT NOT NULL, sent BOOLEAN DEFAULT FALSE NOT NULL, sent_at TIMESTAMP WITH TIME ZONE, error_message VARCHAR(500), created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT patient_notifications_pkey PRIMARY KEY (id), CONSTRAINT fk_patient_notif_patient FOREIGN KEY (patient_id) REFERENCES patients(id));
CREATE INDEX idx_patient_notif_patient ON patient_notifications(patient_id, sent);

--rollback DROP TABLE patient_notifications;
