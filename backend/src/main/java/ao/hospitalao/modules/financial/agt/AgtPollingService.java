package ao.hospitalao.modules.financial.agt;

import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço de polling para consultar o estado das facturas
 * submetidas à AGT e ainda pendentes de resposta.
 *
 * Executa a cada 2 minutos para verificar facturas com
 * agtStatus = PENDING e actualizar para ACEITE ou REJEITADO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgtPollingService {

    private final InvoiceRepository invoiceRepository;
    private final AgtApiClient agtApiClient;

    /**
     * Polling a cada 2 minutos.
     * Verifica facturas pendentes de validação AGT.
     */
    @Scheduled(fixedDelay = 120_000)
    @Transactional
    public void pollPendingInvoices() {
        List<Invoice> pending = invoiceRepository.findByAgtStatusPending(
            PageRequest.of(0, 50));

        if (pending.isEmpty()) return;

        log.info("AGT Polling: checking {} pending invoices", pending.size());

        for (Invoice invoice : pending) {
            try {
                AgtApiClient.AgtStatusResult result =
                    agtApiClient.checkStatus(invoice.getAgtRequestId());

                switch (result.status()) {
                    case "ACEITE", "ACCEPTED" -> {
                        invoice.setAgtStatus("ACEITE");
                        invoice.setAgtValidationCode(result.validationCode());
                        invoice.setAgtQrCode(result.qrCode());
                        log.info("Invoice {} ACEITE by AGT. ValidationCode: {}",
                            invoice.getInvoiceNumber(), result.validationCode());
                    }
                    case "REJEITADO", "REJECTED" -> {
                        invoice.setAgtStatus("REJEITADO");
                        invoice.setAgtErrorMessage(result.message());
                        log.warn("Invoice {} REJEITADO by AGT: {}",
                            invoice.getInvoiceNumber(), result.message());
                    }
                    case "PENDING", "PROCESSANDO" -> {
                        log.debug("Invoice {} still pending at AGT",
                            invoice.getInvoiceNumber());
                    }
                    default -> {
                        log.warn("Unknown AGT status '{}' for invoice {}",
                            result.status(), invoice.getInvoiceNumber());
                    }
                }

                invoiceRepository.save(invoice);

            } catch (Exception e) {
                log.error("Error polling AGT for invoice {}: {}",
                    invoice.getInvoiceNumber(), e.getMessage());
            }
        }
    }
}