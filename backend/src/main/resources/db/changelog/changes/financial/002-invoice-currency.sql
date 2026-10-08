--liquibase formatted sql

-- changeset hospitalao:010-06-invoice-currency splitStatements:false
ALTER TABLE invoices ADD COLUMN currency VARCHAR(3) DEFAULT 'AOA' NOT NULL;

--rollback ALTER TABLE invoices DROP COLUMN currency;
