package ao.hospitalao.modules.financial.agt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class AgtPollingServiceTest {

  @Test
  void persistsAcceptedInvoiceAndAgtValidationMetadata() throws Exception {
    var invoice = pendingInvoice("request-accepted");
    var repository = mock(InvoiceRepository.class);
    var apiClient = mock(AgtApiClient.class);
    when(repository.findByAgtStatusPending(any(Pageable.class))).thenReturn(List.of(invoice));
    when(apiClient.checkStatus("request-accepted"))
        .thenReturn(
            new AgtApiClient.AgtStatusResult("ACCEPTED", "validation-123", "qr-payload", null));

    new AgtPollingService(repository, apiClient).pollPendingInvoices();

    assertThat(invoice.getAgtStatus()).isEqualTo("ACEITE");
    assertThat(invoice.getAgtValidationCode()).isEqualTo("validation-123");
    assertThat(invoice.getAgtQrCode()).isEqualTo("qr-payload");
    verify(repository).save(invoice);
  }

  @Test
  void persistsRejectionReasonAndLeavesPendingInvoicesPending() throws Exception {
    var rejected = pendingInvoice("request-rejected");
    var stillPending = pendingInvoice("request-pending");
    var repository = mock(InvoiceRepository.class);
    var apiClient = mock(AgtApiClient.class);
    when(repository.findByAgtStatusPending(any(Pageable.class)))
        .thenReturn(List.of(rejected, stillPending));
    when(apiClient.checkStatus("request-rejected"))
        .thenReturn(
            new AgtApiClient.AgtStatusResult("REJECTED", null, null, "E01: A validação falhou"));
    when(apiClient.checkStatus("request-pending"))
        .thenReturn(new AgtApiClient.AgtStatusResult("PENDING", null, null, null));

    new AgtPollingService(repository, apiClient).pollPendingInvoices();

    assertThat(rejected.getAgtStatus()).isEqualTo("REJEITADO");
    assertThat(rejected.getAgtErrorMessage()).isEqualTo("E01: A validação falhou");
    assertThat(stillPending.getAgtStatus()).isEqualTo("PENDING");
    verify(repository).save(rejected);
    verify(repository).save(stillPending);
  }

  @Test
  void continuesPollingRemainingInvoicesAfterOneStatusRequestFails() throws Exception {
    var failedRequest = pendingInvoice("request-fails");
    var accepted = pendingInvoice("request-accepted");
    var repository = mock(InvoiceRepository.class);
    var apiClient = mock(AgtApiClient.class);
    when(repository.findByAgtStatusPending(any(Pageable.class)))
        .thenReturn(List.of(failedRequest, accepted));
    when(apiClient.checkStatus("request-fails"))
        .thenThrow(new IllegalStateException("temporary status failure"));
    when(apiClient.checkStatus("request-accepted"))
        .thenReturn(new AgtApiClient.AgtStatusResult("ACCEPTED", null, null, null));

    new AgtPollingService(repository, apiClient).pollPendingInvoices();

    assertThat(failedRequest.getAgtStatus()).isEqualTo("PENDING");
    assertThat(accepted.getAgtStatus()).isEqualTo("ACEITE");
    verify(repository, never()).save(failedRequest);
    verify(repository).save(accepted);
  }

  private Invoice pendingInvoice(String requestId) {
    return Invoice.builder()
        .invoiceNumber("FT " + requestId)
        .agtRequestId(requestId)
        .agtStatus("PENDING")
        .build();
  }
}
