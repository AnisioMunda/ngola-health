package ao.hospitalao.modules.financial.agt;

import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de polling para consultar o estado das facturas submetidas à AGT e ainda pendentes de
 * resposta.
 *
 * <p>Executa a cada 2 minutos para verificar facturas com agtStatus = PENDING e actualizar para
 * ACEITE ou REJEITADO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgtPollingService {

  private final InvoiceRepository invoiceRepository;
  private final AgtApiClient agtApiClient;

  /** Polling a cada 2 minutos. Verifica facturas pendentes de validação AGT. */
  @Scheduled(fixedDelay = 120_000)
  @Transactional
  public void pollPendingInvoices() {
    List<Invoice> pending = invoiceRepository.findByAgtStatusPending(PageRequest.of(0, 50));

    if (pending.isEmpty()) return;

    log.info("AGT Polling: checking {} pending invoices", pending.size());

    for (Invoice invoice : pending) {
      try {
        AgtApiClient.AgtStatusResult result = agtApiClient.checkStatus(invoice.getAgtRequestId());

        switch (result.status()) {
          case "ACEITE", "ACCEPTED" -> {
            invoice.setAgtStatus("ACEITE");
            invoice.setAgtValidationCode(result.validationCode());
            invoice.setAgtQrCode(result.qrCode());
            log.info("Invoice accepted by AGT");
          }
          case "REJEITADO", "REJECTED" -> {
            invoice.setAgtStatus("REJEITADO");
            invoice.setAgtErrorMessage(result.message());
            log.warn("Invoice rejected by AGT");
          }
          case "PENDING", "PROCESSANDO" -> {
            log.debug("Invoice remains pending at AGT");
          }
          default -> {
            log.warn("AGT returned an unknown invoice status");
          }
        }

        invoiceRepository.save(invoice);

      } catch (Exception e) {
        log.error("AGT invoice status polling failed ({})", e.getClass().getSimpleName());
      }
    }
  }
}
