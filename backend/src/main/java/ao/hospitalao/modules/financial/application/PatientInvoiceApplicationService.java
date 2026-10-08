package ao.hospitalao.modules.financial.application;

import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.entity.Invoice.InvoiceStatus;
import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientInvoiceApplicationService {

  private static final List<InvoiceStatus> VISIBLE_STATUSES =
      List.of(
          InvoiceStatus.EMITIDO,
          InvoiceStatus.PAGO_PARCIALMENTE,
          InvoiceStatus.PAGO,
          InvoiceStatus.EM_ATRASO);
  private static final List<InvoiceStatus> OPEN_STATUSES =
      List.of(InvoiceStatus.EMITIDO, InvoiceStatus.PAGO_PARCIALMENTE, InvoiceStatus.EM_ATRASO);

  private final InvoiceRepository invoiceRepository;

  @Transactional(readOnly = true)
  public List<PatientInvoice> findVisibleInvoices(UUID patientId) {
    return invoiceRepository
        .findByPatientIdAndStatusInOrderByIssuedAtDesc(
            patientId, VISIBLE_STATUSES, PageRequest.of(0, 50))
        .stream()
        .map(this::toPatientInvoice)
        .toList();
  }

  @Transactional(readOnly = true)
  public PatientInvoiceSummary getSummary(UUID patientId) {
    return new PatientInvoiceSummary(
        invoiceRepository.countByPatientIdAndStatusIn(patientId, OPEN_STATUSES),
        invoiceRepository.sumBalanceByPatientIdAndStatusIn(patientId, OPEN_STATUSES));
  }

  private PatientInvoice toPatientInvoice(Invoice invoice) {
    return new PatientInvoice(
        invoice.getId(),
        invoice.getInvoiceNumber(),
        invoice.getIssuedAt(),
        invoice.getTotalAmount(),
        invoice.getStatus().name(),
        statusLabel(invoice.getStatus()));
  }

  private String statusLabel(InvoiceStatus status) {
    return switch (status) {
      case EMITIDO -> "Emitida";
      case PAGO_PARCIALMENTE -> "Parcialmente paga";
      case PAGO -> "Paga";
      case EM_ATRASO -> "Em atraso";
      case RASCUNHO -> "Rascunho";
      case ANULADO -> "Anulada";
    };
  }

  public record PatientInvoice(
      UUID id,
      String invoiceNumber,
      OffsetDateTime issueDate,
      BigDecimal totalAmount,
      String status,
      String statusLabel) {}

  public record PatientInvoiceSummary(long pendingInvoices, BigDecimal totalDebt) {}
}
