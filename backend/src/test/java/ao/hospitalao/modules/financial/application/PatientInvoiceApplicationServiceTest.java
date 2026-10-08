package ao.hospitalao.modules.financial.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.entity.Invoice.InvoiceStatus;
import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PatientInvoiceApplicationServiceTest {

  @Mock private InvoiceRepository invoiceRepository;

  @InjectMocks private PatientInvoiceApplicationService service;

  @Test
  void mapsVisibleInvoicesWithoutExposingDraftOrCancelledStatuses() {
    UUID patientId = UUID.randomUUID();
    UUID invoiceId = UUID.randomUUID();
    OffsetDateTime issuedAt = OffsetDateTime.now();
    Invoice invoice = mock(Invoice.class);
    when(invoice.getId()).thenReturn(invoiceId);
    when(invoice.getInvoiceNumber()).thenReturn("INV-100");
    when(invoice.getIssuedAt()).thenReturn(issuedAt);
    when(invoice.getTotalAmount()).thenReturn(BigDecimal.valueOf(1500));
    when(invoice.getStatus()).thenReturn(InvoiceStatus.EMITIDO);
    when(invoiceRepository.findByPatientIdAndStatusInOrderByIssuedAtDesc(
            eq(patientId), anyList(), eq(PageRequest.of(0, 50))))
        .thenReturn(List.of(invoice));

    var invoices = service.findVisibleInvoices(patientId);

    assertThat(invoices)
        .containsExactly(
            new PatientInvoiceApplicationService.PatientInvoice(
                invoiceId, "INV-100", issuedAt, BigDecimal.valueOf(1500), "EMITIDO", "Emitida"));
    verify(invoiceRepository)
        .findByPatientIdAndStatusInOrderByIssuedAtDesc(
            patientId,
            List.of(
                InvoiceStatus.EMITIDO,
                InvoiceStatus.PAGO_PARCIALMENTE,
                InvoiceStatus.PAGO,
                InvoiceStatus.EM_ATRASO),
            PageRequest.of(0, 50));
  }

  @Test
  void summarizesOnlyOpenInvoiceStatuses() {
    UUID patientId = UUID.randomUUID();
    BigDecimal totalDebt = BigDecimal.valueOf(2300);
    when(invoiceRepository.countByPatientIdAndStatusIn(eq(patientId), anyList())).thenReturn(3L);
    when(invoiceRepository.sumBalanceByPatientIdAndStatusIn(eq(patientId), anyList()))
        .thenReturn(totalDebt);

    assertThat(service.getSummary(patientId))
        .isEqualTo(new PatientInvoiceApplicationService.PatientInvoiceSummary(3, totalDebt));

    List<InvoiceStatus> openStatuses =
        List.of(InvoiceStatus.EMITIDO, InvoiceStatus.PAGO_PARCIALMENTE, InvoiceStatus.EM_ATRASO);
    verify(invoiceRepository).countByPatientIdAndStatusIn(patientId, openStatuses);
    verify(invoiceRepository).sumBalanceByPatientIdAndStatusIn(patientId, openStatuses);
  }
}
