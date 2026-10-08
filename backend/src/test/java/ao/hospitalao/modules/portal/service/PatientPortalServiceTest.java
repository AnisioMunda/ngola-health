package ao.hospitalao.modules.portal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.financial.application.PatientInvoiceApplicationService;
import ao.hospitalao.modules.laboratory.application.PatientLabResultApplicationService;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.portal.dto.PortalDtos.PortalLoginRequest;
import ao.hospitalao.modules.portal.dto.PortalDtos.PortalRegisterRequest;
import ao.hospitalao.modules.portal.entity.PatientPortalAccount;
import ao.hospitalao.modules.portal.repository.PatientPortalAccountRepository;
import ao.hospitalao.security.jwt.JwtService;
import ao.hospitalao.security.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PatientPortalServiceTest {

  @Mock private PatientPortalAccountRepository portalAccountRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private ao.hospitalao.modules.episodes.repository.EpisodeRepository episodeRepository;

  @Mock
  private ao.hospitalao.modules.scheduling.repository.AppointmentRepository appointmentRepository;

  @Mock private PatientLabResultApplicationService labResultApplicationService;
  @Mock private PatientInvoiceApplicationService invoiceApplicationService;

  @Mock
  private ao.hospitalao.modules.prescription.repository.PrescriptionRepository
      prescriptionRepository;

  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;

  @InjectMocks private PatientPortalService portalService;

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void registrationCreatesAPendingAccountWithoutIssuingAToken() {
    UUID patientId = UUID.randomUUID();
    Patient patient = Patient.builder().id(patientId).fullName("Patient Name").build();
    when(patientRepository.findAllByPortalIdentifier("CARD-100")).thenReturn(List.of(patient));
    when(portalAccountRepository.existsByPatientId(patientId)).thenReturn(false);
    when(portalAccountRepository.existsByEmailIgnoreCase("patient@example.com")).thenReturn(false);
    when(passwordEncoder.encode("SecurePassword123")).thenReturn("encoded-password");

    PortalRegisterRequest request = new PortalRegisterRequest();
    request.setPatientNumber(" CARD-100 ");
    request.setEmail("Patient@Example.com");
    request.setPassword("SecurePassword123");

    var response = portalService.register(request);

    assertThat(response.getStatus()).isEqualTo("PENDING_APPROVAL");
    ArgumentCaptor<PatientPortalAccount> accountCaptor =
        ArgumentCaptor.forClass(PatientPortalAccount.class);
    verify(portalAccountRepository).save(accountCaptor.capture());
    assertThat(accountCaptor.getValue().getEmail()).isEqualTo("patient@example.com");
    assertThat(accountCaptor.getValue().isActive()).isFalse();
    assertThat(accountCaptor.getValue().isEmailVerified()).isFalse();
    verify(jwtService, never()).generatePortalToken(any(UUID.class), anyString(), any(UUID.class));
  }

  @Test
  void loginRejectsAnAccountUntilTheHospitalApprovesIt() {
    PatientPortalAccount account =
        PatientPortalAccount.builder()
            .email("patient@example.com")
            .passwordHash("encoded-password")
            .build();
    when(portalAccountRepository.findByEmailIgnoreCase("patient@example.com"))
        .thenReturn(Optional.of(account));

    PortalLoginRequest request = new PortalLoginRequest();
    request.setEmail("Patient@Example.com");
    request.setPassword("SecurePassword123");

    assertThatThrownBy(() -> portalService.login(request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("aguarda confirmação");
    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  @Test
  void approvalOnlyFindsPendingAccountsInTheCurrentHospital() {
    UUID hospitalId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();
    Patient patient = Patient.builder().id(UUID.randomUUID()).fullName("Patient Name").build();
    patient.setHospitalId(hospitalId);
    PatientPortalAccount account =
        PatientPortalAccount.builder()
            .id(accountId)
            .patient(patient)
            .email("patient@example.com")
            .passwordHash("encoded-password")
            .build();
    TenantContext.setCurrentHospital(hospitalId);
    when(portalAccountRepository.findByIdAndPatient_HospitalIdAndEmailVerifiedFalse(
            accountId, hospitalId))
        .thenReturn(Optional.of(account));
    when(portalAccountRepository.findByEmailIgnoreCase("patient@example.com"))
        .thenReturn(Optional.of(account));
    when(passwordEncoder.matches("SecurePassword123", "encoded-password")).thenReturn(true);
    when(jwtService.generatePortalToken(patient.getId(), "patient@example.com", hospitalId))
        .thenReturn("portal-token");

    portalService.approveAccount(accountId);

    assertThat(account.isActive()).isTrue();
    assertThat(account.isEmailVerified()).isTrue();
    verify(portalAccountRepository).save(account);

    PortalLoginRequest request = new PortalLoginRequest();
    request.setEmail("patient@example.com");
    request.setPassword("SecurePassword123");

    assertThat(portalService.login(request).getToken()).isEqualTo("portal-token");
  }

  @Test
  void labResultsOnlyExposeCompletedItemsForTheAuthenticatedPatient() {
    UUID patientId = UUID.randomUUID();
    OffsetDateTime resultedAt = OffsetDateTime.now();
    var labResult =
        new PatientLabResultApplicationService.PatientLabResult(
            UUID.randomUUID(),
            "Hemoglobina",
            "COMPLETED",
            "13.2 g/dL",
            "12 - 16 g/dL",
            "Dr. Test",
            resultedAt,
            resultedAt);
    when(labResultApplicationService.findCompletedResults(patientId))
        .thenReturn(List.of(labResult));

    var results = portalService.getLabResults(patientId);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().getExamName()).isEqualTo("Hemoglobina");
    assertThat(results.getFirst().getResult()).isEqualTo("13.2 g/dL");
    assertThat(results.getFirst().getReferenceValues()).isEqualTo("12 - 16 g/dL");
    assertThat(results.getFirst().getResultAt()).isEqualTo(resultedAt);
    assertThat(results.getFirst().getDoctorName()).isEqualTo("Dr. Test");
    verify(labResultApplicationService).findCompletedResults(patientId);
  }

  @Test
  void invoicesAreMappedFromTheFinancialApplicationBoundary() {
    UUID patientId = UUID.randomUUID();
    UUID invoiceId = UUID.randomUUID();
    OffsetDateTime issueDate = OffsetDateTime.now();
    var invoice =
        new PatientInvoiceApplicationService.PatientInvoice(
            invoiceId, "INV-100", issueDate, BigDecimal.valueOf(1500), "EMITIDO", "Emitida");
    when(invoiceApplicationService.findVisibleInvoices(patientId)).thenReturn(List.of(invoice));

    var invoices = portalService.getInvoices(patientId);

    assertThat(invoices).hasSize(1);
    assertThat(invoices.getFirst().getId()).isEqualTo(invoiceId);
    assertThat(invoices.getFirst().getInvoiceNumber()).isEqualTo("INV-100");
    assertThat(invoices.getFirst().getIssueDate()).isEqualTo(issueDate);
    assertThat(invoices.getFirst().getStatusLabel()).isEqualTo("Emitida");
    verify(invoiceApplicationService).findVisibleInvoices(patientId);
  }
}
