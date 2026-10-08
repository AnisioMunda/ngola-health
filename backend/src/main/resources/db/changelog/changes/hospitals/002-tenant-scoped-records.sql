--liquibase formatted sql

--changeset hospitalao:hospitals-002-required-patient-episode-tenants
ALTER TABLE patients ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE episodes ALTER COLUMN hospital_id SET NOT NULL;
--rollback ALTER TABLE episodes ALTER COLUMN hospital_id DROP NOT NULL;
--rollback ALTER TABLE patients ALTER COLUMN hospital_id DROP NOT NULL;

--changeset hospitalao:hospitals-003-invoice-item-tenant
ALTER TABLE invoice_items ADD COLUMN hospital_id UUID;
UPDATE invoice_items i SET hospital_id = inv.hospital_id FROM invoices inv WHERE inv.id = i.invoice_id;
ALTER TABLE invoice_items ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE invoice_items ADD CONSTRAINT fk_invoice_items_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
CREATE INDEX idx_invoice_items_hospital ON invoice_items(hospital_id);
--rollback DROP INDEX idx_invoice_items_hospital;
--rollback ALTER TABLE invoice_items DROP CONSTRAINT fk_invoice_items_hospital;
--rollback ALTER TABLE invoice_items ALTER COLUMN hospital_id DROP NOT NULL;
--rollback ALTER TABLE invoice_items DROP COLUMN hospital_id;

--changeset hospitalao:hospitals-004-payment-tenant
ALTER TABLE payments ADD COLUMN hospital_id UUID;
UPDATE payments p SET hospital_id = i.hospital_id FROM invoices i WHERE i.id = p.invoice_id;
ALTER TABLE payments ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE payments ADD CONSTRAINT fk_payments_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
CREATE INDEX idx_payments_hospital ON payments(hospital_id);
--rollback DROP INDEX idx_payments_hospital;
--rollback ALTER TABLE payments DROP CONSTRAINT fk_payments_hospital;
--rollback ALTER TABLE payments ALTER COLUMN hospital_id DROP NOT NULL;
--rollback ALTER TABLE payments DROP COLUMN hospital_id;

--changeset hospitalao:hospitals-005-prescription-item-tenant
ALTER TABLE prescription_items ADD COLUMN hospital_id UUID;
UPDATE prescription_items i SET hospital_id = p.hospital_id FROM prescriptions p WHERE p.id = i.prescription_id;
ALTER TABLE prescription_items ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE prescription_items ADD CONSTRAINT fk_prescription_items_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
CREATE INDEX idx_prescription_items_hospital ON prescription_items(hospital_id);
--rollback DROP INDEX idx_prescription_items_hospital;
--rollback ALTER TABLE prescription_items DROP CONSTRAINT fk_prescription_items_hospital;
--rollback ALTER TABLE prescription_items ALTER COLUMN hospital_id DROP NOT NULL;
--rollback ALTER TABLE prescription_items DROP COLUMN hospital_id;

--changeset hospitalao:hospitals-006-dispensation-tenant
ALTER TABLE dispensations ADD COLUMN hospital_id UUID;
UPDATE dispensations d SET hospital_id = p.hospital_id
FROM prescription_items pi JOIN prescriptions p ON p.id = pi.prescription_id
WHERE pi.id = d.prescription_item_id;
ALTER TABLE dispensations ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE dispensations ADD CONSTRAINT fk_dispensations_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
CREATE INDEX idx_dispensations_hospital ON dispensations(hospital_id);
--rollback DROP INDEX idx_dispensations_hospital;
--rollback ALTER TABLE dispensations DROP CONSTRAINT fk_dispensations_hospital;
--rollback ALTER TABLE dispensations ALTER COLUMN hospital_id DROP NOT NULL;
--rollback ALTER TABLE dispensations DROP COLUMN hospital_id;

--changeset hospitalao:hospitals-007-stock-batch-tenant
ALTER TABLE stock_batches ADD COLUMN hospital_id UUID;
UPDATE stock_batches b SET hospital_id = m.hospital_id FROM medications m WHERE m.id = b.medication_id;
ALTER TABLE stock_batches ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE stock_batches ADD CONSTRAINT fk_stock_batches_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
CREATE INDEX idx_stock_batches_hospital ON stock_batches(hospital_id);
--rollback DROP INDEX idx_stock_batches_hospital;
--rollback ALTER TABLE stock_batches DROP CONSTRAINT fk_stock_batches_hospital;
--rollback ALTER TABLE stock_batches ALTER COLUMN hospital_id DROP NOT NULL;
--rollback ALTER TABLE stock_batches DROP COLUMN hospital_id;

--changeset hospitalao:hospitals-008-stock-movement-tenant
ALTER TABLE stock_movements ADD COLUMN hospital_id UUID;
UPDATE stock_movements sm SET hospital_id = m.hospital_id
FROM stock_batches b JOIN medications m ON m.id = b.medication_id
WHERE b.id = sm.batch_id;
ALTER TABLE stock_movements ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movements_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
CREATE INDEX idx_stock_movements_hospital ON stock_movements(hospital_id);
--rollback DROP INDEX idx_stock_movements_hospital;
--rollback ALTER TABLE stock_movements DROP CONSTRAINT fk_stock_movements_hospital;
--rollback ALTER TABLE stock_movements ALTER COLUMN hospital_id DROP NOT NULL;
--rollback ALTER TABLE stock_movements DROP COLUMN hospital_id;

--changeset hospitalao:hospitals-009-maintenance-tenant
ALTER TABLE maintenance_records ADD COLUMN hospital_id UUID;
UPDATE maintenance_records r SET hospital_id = e.hospital_id FROM equipment e WHERE e.id = r.equipment_id;
ALTER TABLE maintenance_records ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE maintenance_records ADD CONSTRAINT fk_maintenance_records_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
CREATE INDEX idx_maintenance_records_hospital ON maintenance_records(hospital_id);
--rollback DROP INDEX idx_maintenance_records_hospital;
--rollback ALTER TABLE maintenance_records DROP CONSTRAINT fk_maintenance_records_hospital;
--rollback ALTER TABLE maintenance_records ALTER COLUMN hospital_id DROP NOT NULL;
--rollback ALTER TABLE maintenance_records DROP COLUMN hospital_id;

--changeset hospitalao:hospitals-010-lab-request-item-tenant
ALTER TABLE lab_request_items ADD COLUMN hospital_id UUID;
UPDATE lab_request_items i SET hospital_id = r.hospital_id FROM lab_requests r WHERE r.id = i.request_id;
ALTER TABLE lab_request_items ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE lab_request_items ADD CONSTRAINT fk_lab_request_items_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
CREATE INDEX idx_lab_request_items_hospital ON lab_request_items(hospital_id);
--rollback DROP INDEX idx_lab_request_items_hospital;
--rollback ALTER TABLE lab_request_items DROP CONSTRAINT fk_lab_request_items_hospital;
--rollback ALTER TABLE lab_request_items ALTER COLUMN hospital_id DROP NOT NULL;
--rollback ALTER TABLE lab_request_items DROP COLUMN hospital_id;

--changeset hospitalao:hospitals-011-bed-transfer-tenant
ALTER TABLE bed_transfers ADD COLUMN hospital_id UUID;
UPDATE bed_transfers t SET hospital_id = a.hospital_id FROM admissions a WHERE a.id = t.admission_id;
ALTER TABLE bed_transfers ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE bed_transfers ADD CONSTRAINT fk_bed_transfers_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
CREATE INDEX idx_bed_transfers_hospital ON bed_transfers(hospital_id);
--rollback DROP INDEX idx_bed_transfers_hospital;
--rollback ALTER TABLE bed_transfers DROP CONSTRAINT fk_bed_transfers_hospital;
--rollback ALTER TABLE bed_transfers ALTER COLUMN hospital_id DROP NOT NULL;
--rollback ALTER TABLE bed_transfers DROP COLUMN hospital_id;

--changeset hospitalao:hospitals-012-patient-notification-tenant
ALTER TABLE patient_notifications ADD COLUMN hospital_id UUID;
UPDATE patient_notifications n SET hospital_id = p.hospital_id FROM patients p WHERE p.id = n.patient_id;
ALTER TABLE patient_notifications ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE patient_notifications ADD CONSTRAINT fk_patient_notifications_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
CREATE INDEX idx_patient_notifications_hospital ON patient_notifications(hospital_id);
--rollback DROP INDEX idx_patient_notifications_hospital;
--rollback ALTER TABLE patient_notifications DROP CONSTRAINT fk_patient_notifications_hospital;
--rollback ALTER TABLE patient_notifications ALTER COLUMN hospital_id DROP NOT NULL;
--rollback ALTER TABLE patient_notifications DROP COLUMN hospital_id;

--changeset hospitalao:hospitals-013-patient-portal-token-tenant
ALTER TABLE patient_portal_tokens ADD COLUMN hospital_id UUID;
UPDATE patient_portal_tokens t SET hospital_id = p.hospital_id
FROM patient_portal_accounts a JOIN patients p ON p.id = a.patient_id
WHERE a.id = t.account_id;
ALTER TABLE patient_portal_tokens ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE patient_portal_tokens ADD CONSTRAINT fk_patient_portal_tokens_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
CREATE INDEX idx_patient_portal_tokens_hospital ON patient_portal_tokens(hospital_id);
--rollback DROP INDEX idx_patient_portal_tokens_hospital;
--rollback ALTER TABLE patient_portal_tokens DROP CONSTRAINT fk_patient_portal_tokens_hospital;
--rollback ALTER TABLE patient_portal_tokens ALTER COLUMN hospital_id DROP NOT NULL;
--rollback ALTER TABLE patient_portal_tokens DROP COLUMN hospital_id;
