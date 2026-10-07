--liquibase formatted sql

-- changeset hospitalao:010-01-criar-tabela-service-prices splitStatements:false
CREATE TABLE service_prices (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, code VARCHAR(30) NOT NULL, description VARCHAR(300) NOT NULL, category VARCHAR(50) NOT NULL, unit_price numeric(12, 2) NOT NULL, vat_rate numeric(5, 2) DEFAULT 0 NOT NULL, active BOOLEAN DEFAULT TRUE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT service_prices_pkey PRIMARY KEY (id), CONSTRAINT fk_service_prices_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id));
ALTER TABLE service_prices ADD CONSTRAINT uq_service_prices_hospital_code UNIQUE (hospital_id, code);
CREATE INDEX idx_service_prices_hospital ON service_prices(hospital_id);

--rollback DROP TABLE service_prices;

-- changeset hospitalao:010-02-criar-tabela-invoices splitStatements:false
CREATE TABLE invoices (id UUID DEFAULT uuid_generate_v4() NOT NULL, hospital_id UUID NOT NULL, invoice_number VARCHAR(30) NOT NULL, document_type VARCHAR(5) DEFAULT 'FR' NOT NULL, patient_id UUID NOT NULL, episode_id UUID, status VARCHAR(25) DEFAULT 'RASCUNHO' NOT NULL, payment_method VARCHAR(30), patient_nif VARCHAR(14), patient_fiscal_name VARCHAR(200), subtotal numeric(12, 2) DEFAULT 0 NOT NULL, discount_amount numeric(12, 2) DEFAULT 0 NOT NULL, vat_amount numeric(12, 2) DEFAULT 0 NOT NULL, total_amount numeric(12, 2) DEFAULT 0 NOT NULL, paid_amount numeric(12, 2) DEFAULT 0 NOT NULL, insurance_provider VARCHAR(100), insurance_policy_number VARCHAR(50), insurance_coverage_percent numeric(5, 2) DEFAULT 0, issued_at TIMESTAMP WITH TIME ZONE, due_date date, paid_at TIMESTAMP WITH TIME ZONE, notes TEXT, created_by UUID, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT invoices_pkey PRIMARY KEY (id), CONSTRAINT fk_invoices_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id), CONSTRAINT fk_invoices_episode FOREIGN KEY (episode_id) REFERENCES episodes(id), CONSTRAINT fk_invoices_created_by FOREIGN KEY (created_by) REFERENCES users(id), CONSTRAINT fk_invoices_patient FOREIGN KEY (patient_id) REFERENCES patients(id), CONSTRAINT uq_invoice_number UNIQUE (invoice_number));
CREATE INDEX idx_invoices_patient ON invoices(patient_id);
CREATE INDEX idx_invoices_hospital ON invoices(hospital_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_document_type ON invoices(document_type);

--rollback DROP TABLE invoices;

-- changeset hospitalao:010-03-criar-tabela-invoice-items splitStatements:false
CREATE TABLE invoice_items (id UUID DEFAULT uuid_generate_v4() NOT NULL, invoice_id UUID NOT NULL, service_price_id UUID, description VARCHAR(300) NOT NULL, quantity INTEGER DEFAULT 1 NOT NULL, unit_price numeric(12, 2) NOT NULL, discount_percent numeric(5, 2) DEFAULT 0, vat_rate numeric(5, 2) DEFAULT 0, line_total numeric(12, 2) NOT NULL, CONSTRAINT invoice_items_pkey PRIMARY KEY (id), CONSTRAINT fk_invoice_items_service FOREIGN KEY (service_price_id) REFERENCES service_prices(id), CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id));
CREATE INDEX idx_invoice_items_invoice ON invoice_items(invoice_id);

--rollback DROP TABLE invoice_items;

-- changeset hospitalao:010-04-criar-tabela-payments splitStatements:true
CREATE TABLE payments (id UUID DEFAULT uuid_generate_v4() NOT NULL, invoice_id UUID NOT NULL, amount numeric(12, 2) NOT NULL, payment_method VARCHAR(30) NOT NULL, reference VARCHAR(100), paid_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, received_by UUID, notes VARCHAR(500), CONSTRAINT payments_pkey PRIMARY KEY (id), CONSTRAINT fk_payments_user FOREIGN KEY (received_by) REFERENCES users(id), CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id));
COMMENT ON COLUMN payments.reference IS 'Referência bancária, nº cheque, código Multicaixa Express, etc.';
CREATE INDEX idx_payments_invoice ON payments(invoice_id);

--rollback DROP TABLE payments;

-- changeset hospitalao:010-05-invoice-sequences splitStatements:true
CREATE SEQUENCE IF NOT EXISTS seq_ft START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_fr START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_nc START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_nd START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_rc START 1 INCREMENT 1;

--rollback DROP SEQUENCE IF EXISTS seq_ft;
--rollback DROP SEQUENCE IF EXISTS seq_fr;
--rollback DROP SEQUENCE IF EXISTS seq_nc;
--rollback DROP SEQUENCE IF EXISTS seq_nd;
--rollback DROP SEQUENCE IF EXISTS seq_rc;
