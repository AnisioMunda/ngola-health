--liquibase formatted sql

-- changeset hospitalao:011-01-agt-campos-invoice splitStatements:true
ALTER TABLE invoices ADD agt_request_id VARCHAR(100);
COMMENT ON COLUMN invoices.agt_request_id IS 'requestID devolvido pela API AGT após submissão';
ALTER TABLE invoices ADD agt_status VARCHAR(20) DEFAULT 'NAO_SUBMETIDO';
COMMENT ON COLUMN invoices.agt_status IS 'Estado AGT: NAO_SUBMETIDO, PENDING, ACEITE, REJEITADO';
ALTER TABLE invoices ADD agt_validation_code VARCHAR(100);
COMMENT ON COLUMN invoices.agt_validation_code IS 'Código de validação atribuído pela AGT (após ACEITE)';
ALTER TABLE invoices ADD agt_qr_code TEXT;
COMMENT ON COLUMN invoices.agt_qr_code IS 'Dados do QR Code a imprimir no documento fiscal';
ALTER TABLE invoices ADD agt_error_message VARCHAR(500);
COMMENT ON COLUMN invoices.agt_error_message IS 'Mensagem de erro da AGT em caso de rejeição';
ALTER TABLE invoices ADD agt_submitted_at TIMESTAMP WITH TIME ZONE;
COMMENT ON COLUMN invoices.agt_submitted_at IS 'Data/hora de submissão do documento à API AGT';
CREATE INDEX idx_invoices_agt_status ON invoices(agt_status);

--rollback ALTER TABLE invoices DROP COLUMN agt_request_id;
--rollback ALTER TABLE invoices DROP COLUMN agt_status;
--rollback ALTER TABLE invoices DROP COLUMN agt_validation_code;
--rollback ALTER TABLE invoices DROP COLUMN agt_qr_code;
--rollback ALTER TABLE invoices DROP COLUMN agt_error_message;
--rollback ALTER TABLE invoices DROP COLUMN agt_submitted_at;
