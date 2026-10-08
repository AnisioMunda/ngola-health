package ao.hospitalao.modules.financial.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.financial.agt.AgtApiClient;
import ao.hospitalao.modules.financial.dto.FinancialDtos.CreateInvoiceItemRequest;
import ao.hospitalao.modules.financial.dto.FinancialDtos.CreateInvoiceRequest;
import ao.hospitalao.modules.financial.dto.FinancialDtos.RegisterPaymentRequest;
import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.entity.Invoice.InvoiceStatus;
import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import ao.hospitalao.modules.financial.repository.ServicePriceRepository;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FinancialServiceTest {

  @Mock private InvoiceRepository invoiceRepository;
  @Mock private ServicePriceRepository servicePriceRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private EpisodeRepository episodeRepository;
  @Mock private UserRepository userRepository;
  @Mock private HospitalRepository hospitalRepository;
  @Mock private AgtApiClient agtApiClient;

  @InjectMocks private FinancialService financialService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createRoundsDiscountVatAndInvoiceTotalsHalfUpToAoaCents() {
    UUID hospitalId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    Patient patient = mock(Patient.class);
    when(patient.getId()).thenReturn(patientId);
    when(patient.getFullName()).thenReturn("Paciente Teste");
    when(patientRepository.findByHospitalIdAndId(hospitalId, patientId))
        .thenReturn(Optional.of(patient));
    when(hospitalRepository.getReferenceById(hospitalId)).thenReturn(mock(Hospital.class));
    when(invoiceRepository.nextSequence("FT")).thenReturn(1L);
    when(userRepository.findByUsername("billing")).thenReturn(Optional.empty());
    when(invoiceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("billing", "test"));

    var request = new CreateInvoiceRequest();
    request.setPatientId(patientId);
    request.setDocumentType(Invoice.DocumentType.FT);
    request.setItems(
        List.of(
            itemRequest(
                "Consulta",
                new BigDecimal("0.05"),
                new BigDecimal("10.00"),
                new BigDecimal("12.50"))));

    try (var tenant = mockStatic(TenantContext.class)) {
      tenant.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

      var response = financialService.create(request);

      assertThat(response.getCurrency()).isEqualTo("AOA");
      assertThat(response.getSubtotal()).isEqualByComparingTo("0.04");
      assertThat(response.getVatAmount()).isEqualByComparingTo("0.01");
      assertThat(response.getTotalAmount()).isEqualByComparingTo("0.05");
      assertThat(response.getItems())
          .singleElement()
          .satisfies(item -> assertThat(item.getLineTotal()).isEqualByComparingTo("0.05"));
    }
  }

  @Test
  void createRejectsInvoicesWithoutItemsAsBadRequest() {
    UUID hospitalId = UUID.randomUUID();
    var request = new CreateInvoiceRequest();
    request.setPatientId(UUID.randomUUID());
    request.setItems(List.of());

    try (var tenant = mockStatic(TenantContext.class)) {
      tenant.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

      assertThatThrownBy(() -> financialService.create(request))
          .isInstanceOfSatisfying(
              ResponseStatusException.class,
              exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    verify(invoiceRepository, never()).save(any());
  }

  @Test
  void registerPaymentUpdatesPaidAmountAndStatusUsingCurrencyPrecision() {
    UUID invoiceId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    Patient patient = mock(Patient.class);
    when(patient.getId()).thenReturn(patientId);
    when(patient.getFullName()).thenReturn("Paciente Teste");

    Invoice invoice =
        Invoice.builder()
            .id(invoiceId)
            .invoiceNumber("FR 2026/0000001")
            .patient(patient)
            .status(InvoiceStatus.EMITIDO)
            .subtotal(new BigDecimal("0.05"))
            .discountAmount(BigDecimal.ZERO)
            .vatAmount(BigDecimal.ZERO)
            .totalAmount(new BigDecimal("0.05"))
            .paidAmount(BigDecimal.ZERO)
            .items(new ArrayList<>())
            .payments(new ArrayList<>())
            .build();
    when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
    when(userRepository.findByUsername("billing")).thenReturn(Optional.empty());
    when(invoiceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("billing", "test"));

    var request = new RegisterPaymentRequest();
    request.setAmount(new BigDecimal("0.05"));
    request.setPaymentMethod(Invoice.PaymentMethod.NUMERARIO);

    var response = financialService.registerPayment(invoiceId, request);

    assertThat(response.getPaidAmount()).isEqualByComparingTo("0.05");
    assertThat(response.getBalance()).isEqualByComparingTo("0.00");
    assertThat(response.getStatus()).isEqualTo(InvoiceStatus.PAGO);
    assertThat(response.getPayments())
        .singleElement()
        .satisfies(payment -> assertThat(payment.getAmount()).isEqualByComparingTo("0.05"));
  }

  private CreateInvoiceItemRequest itemRequest(
      String description, BigDecimal unitPrice, BigDecimal discountPercent, BigDecimal vatRate) {
    var request = new CreateInvoiceItemRequest();
    request.setDescription(description);
    request.setUnitPrice(unitPrice);
    request.setQuantity(1);
    request.setDiscountPercent(discountPercent);
    request.setVatRate(vatRate);
    return request;
  }
}
