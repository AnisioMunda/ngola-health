--liquibase formatted sql

-- changeset hospitalao:021-01-manual-patient-portal-account-approval
ALTER TABLE patient_portal_accounts ALTER COLUMN active SET DEFAULT FALSE;

--rollback ALTER TABLE patient_portal_accounts ALTER COLUMN active SET DEFAULT TRUE;
