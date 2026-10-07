--liquibase formatted sql

-- changeset hospitalao:002-01-criar-tabela-roles splitStatements:false
CREATE TABLE roles (id UUID DEFAULT uuid_generate_v4() NOT NULL, name VARCHAR(50) NOT NULL, description VARCHAR(255), active BOOLEAN DEFAULT TRUE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT roles_pkey PRIMARY KEY (id), CONSTRAINT uq_roles_name UNIQUE (name));

--rollback DROP TABLE roles;

-- changeset hospitalao:002-02-criar-tabela-permissions splitStatements:false
CREATE TABLE permissions (id UUID DEFAULT uuid_generate_v4() NOT NULL, name VARCHAR(100) NOT NULL, description VARCHAR(255), created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT permissions_pkey PRIMARY KEY (id), CONSTRAINT uq_permissions_name UNIQUE (name));

--rollback DROP TABLE permissions;

-- changeset hospitalao:002-03-criar-tabela-users splitStatements:false
CREATE TABLE users (id UUID DEFAULT uuid_generate_v4() NOT NULL, full_name VARCHAR(200) NOT NULL, username VARCHAR(100) NOT NULL, password_hash VARCHAR(255) NOT NULL, email VARCHAR(200), phone VARCHAR(20), especiality VARCHAR(100), professional_card_number VARCHAR(50), register_status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL, must_change_password BOOLEAN DEFAULT TRUE NOT NULL, last_login TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT users_pkey PRIMARY KEY (id), CONSTRAINT uq_users_username UNIQUE (username), CONSTRAINT uq_users_email UNIQUE (email));
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_register_status ON users(register_status);

--rollback DROP TABLE users;

-- changeset hospitalao:002-04-criar-tabela-user-roles splitStatements:false
CREATE TABLE user_roles (user_id UUID NOT NULL, role_id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE);
ALTER TABLE user_roles ADD CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id);

--rollback DROP TABLE user_roles;

-- changeset hospitalao:002-05-criar-tabela-role-permissions splitStatements:false
CREATE TABLE role_permissions (role_id UUID NOT NULL, permission_id UUID NOT NULL, CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE, CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE);
ALTER TABLE role_permissions ADD CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id);

--rollback DROP TABLE role_permissions;

-- changeset hospitalao:002-06-inserir-roles-base splitStatements:true
INSERT INTO roles (id, name, description, active) VALUES ('00000000-0000-0000-0000-000000000001', 'ADMIN', 'System administrator — full access', TRUE);
INSERT INTO roles (id, name, description, active) VALUES ('00000000-0000-0000-0000-000000000002', 'DOCTOR', 'Doctor — consultations, prescriptions, diagnoses', TRUE);
INSERT INTO roles (id, name, description, active) VALUES ('00000000-0000-0000-0000-000000000003', 'NURSE', 'Nurse — triage, inpatient, daily evolution', TRUE);
INSERT INTO roles (id, name, description, active) VALUES ('00000000-0000-0000-0000-000000000004', 'RECEPTIONIST', 'Receptionist — admission, scheduling, queues', TRUE);
INSERT INTO roles (id, name, description, active) VALUES ('00000000-0000-0000-0000-000000000005', 'PHARMACIST', 'Pharmacist — dispensing and medicine stock', TRUE);
INSERT INTO roles (id, name, description, active) VALUES ('00000000-0000-0000-0000-000000000006', 'FINANCIAL', 'Financial — billing, payments, reports', TRUE);
INSERT INTO roles (id, name, description, active) VALUES ('00000000-0000-0000-0000-000000000007', 'MANAGER', 'Manager — dashboards and reports (read-only)', TRUE);
INSERT INTO roles (id, name, description, active) VALUES ('00000000-0000-0000-0000-000000000008', 'LAB_TECHNICIAN', 'Lab Technician — exams and results', TRUE);

--rollback DELETE FROM roles WHERE id IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000008');
