--liquibase formatted sql

-- changeset hospitalao:006-01-episode-status-enum splitStatements:false
DO $$ BEGIN
                CREATE TYPE episode_status_enum AS ENUM (
                    'SCHEDULED',
                    'IN_PROGRESS',
                    'COMPLETED',
                    'CANCELLED'
                );
            EXCEPTION
                WHEN duplicate_object THEN NULL;
            END $$;

--rollback DROP TYPE IF EXISTS episode_status_enum;

-- changeset hospitalao:006-02-criar-tabela-episodes splitStatements:false
CREATE TABLE episodes (id UUID DEFAULT uuid_generate_v4() NOT NULL, patient_id UUID NOT NULL, doctor_id UUID, episode_type VARCHAR(30) NOT NULL, status VARCHAR(20) DEFAULT 'SCHEDULED' NOT NULL, scheduled_at TIMESTAMP WITH TIME ZONE, started_at TIMESTAMP WITH TIME ZONE, completed_at TIMESTAMP WITH TIME ZONE, reason VARCHAR(500), symptoms TEXT, diagnosis TEXT, prescription TEXT, notes TEXT, blood_pressure VARCHAR(20), heart_rate INTEGER, temperature numeric(4, 1), weight_kg numeric(5, 2), created_by UUID, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT episodes_pkey PRIMARY KEY (id), CONSTRAINT fk_episodes_doctor FOREIGN KEY (doctor_id) REFERENCES users(id), CONSTRAINT fk_episodes_created_by FOREIGN KEY (created_by) REFERENCES users(id), CONSTRAINT fk_episodes_patient FOREIGN KEY (patient_id) REFERENCES patients(id));
CREATE INDEX idx_episodes_patient ON episodes(patient_id);
CREATE INDEX idx_episodes_doctor ON episodes(doctor_id);
CREATE INDEX idx_episodes_status ON episodes(status);
CREATE INDEX idx_episodes_scheduled_at ON episodes(scheduled_at);

--rollback DROP TABLE episodes;
