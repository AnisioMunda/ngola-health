package ao.hospitalao.modules.portal.service;

import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.financial.application.PatientInvoiceApplicationService;
import ao.hospitalao.modules.laboratory.application.PatientLabResultApplicationService;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.portal.dto.PortalDtos.*;
import ao.hospitalao.modules.portal.entity.PatientPortalAccount;
import ao.hospitalao.modules.portal.repository.PatientPortalAccountRepository;
import ao.hospitalao.modules.prescription.repository.PrescriptionRepository;
import ao.hospitalao.modules.scheduling.repository.AppointmentRepository;
import ao.hospitalao.security.jwt.JwtService;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientPortalService {

  private final PatientPortalAccountRepository portalAccountRepository;
  private final PatientRepository patientRepository;
  private final EpisodeRepository episodeRepository;
  private final AppointmentRepository appointmentRepository;
  private final PatientLabResultApplicationService labResultApplicationService;
  private final PatientInvoiceApplicationService invoiceApplicationService;
  private final PrescriptionRepository prescriptionRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  // ------------------------------------------------
  // Registo
  // ------------------------------------------------

  @Transactional
  public PortalRegistrationResponse register(PortalRegisterRequest req) {
    List<Patient> matches =
        patientRepository.findAllByPortalIdentifier(req.getPatientNumber().trim());
    if (matches.isEmpty()) {
      throw new EntityNotFoundException(
          "Paciente não encontrado. Contacte a recepção do hospital.");
    }
    if (matches.size() > 1) {
      throw new IllegalStateException(
          "Identificador associado a mais de um registo. Contacte a recepção do hospital.");
    }
    Patient patient = matches.getFirst();
    String email = normalizeEmail(req.getEmail());

    if (portalAccountRepository.existsByPatientId(patient.getId())) {
      throw new IllegalStateException(
          "Já existe uma conta do portal para este paciente. Contacte a recepção do hospital.");
    }
    if (portalAccountRepository.existsByEmailIgnoreCase(email)) {
      throw new IllegalStateException("Este email já está registado.");
    }

    PatientPortalAccount account =
        PatientPortalAccount.builder()
            .patient(patient)
            .email(email)
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .build();

    portalAccountRepository.save(account);
    log.info("Portal account registration awaits hospital approval");

    return PortalRegistrationResponse.builder()
        .status("PENDING_APPROVAL")
        .message("Registo recebido. O hospital tem de confirmar a sua identidade antes do acesso.")
        .build();
  }

  @Transactional(readOnly = true)
  public List<PortalPendingAccountDto> getPendingApprovals() {
    UUID hospitalId = requireCurrentHospital();
    return portalAccountRepository
        .findAllByPatient_HospitalIdAndEmailVerifiedFalseOrderByCreatedAtAsc(hospitalId)
        .stream()
        .map(
            account ->
                PortalPendingAccountDto.builder()
                    .accountId(account.getId())
                    .patientId(account.getPatient().getId())
                    .patientName(account.getPatient().getFullName())
                    .email(account.getEmail())
                    .requestedAt(account.getCreatedAt())
                    .build())
        .toList();
  }

  @Transactional
  public void approveAccount(UUID accountId) {
    UUID hospitalId = requireCurrentHospital();
    PatientPortalAccount account =
        portalAccountRepository
            .findByIdAndPatient_HospitalIdAndEmailVerifiedFalse(accountId, hospitalId)
            .orElseThrow(
                () -> new EntityNotFoundException("Pedido de acesso pendente não encontrado."));

    account.setActive(true);
    account.setEmailVerified(true);
    portalAccountRepository.save(account);
    log.info("Patient portal account approved");
  }

  // ------------------------------------------------
  // Login
  // ------------------------------------------------

  @Transactional
  public PortalLoginResponse login(PortalLoginRequest req) {
    PatientPortalAccount account =
        portalAccountRepository
            .findByEmailIgnoreCase(normalizeEmail(req.getEmail()))
            .orElseThrow(() -> new IllegalArgumentException("Email ou password incorrectos."));

    if (!account.isActive() || !account.isEmailVerified()) {
      throw new IllegalStateException("A conta aguarda confirmação de identidade pelo hospital.");
    }
    if (!passwordEncoder.matches(req.getPassword(), account.getPasswordHash())) {
      throw new IllegalArgumentException("Email ou password incorrectos.");
    }

    account.setLastLoginAt(OffsetDateTime.now());
    portalAccountRepository.save(account);

    String token =
        jwtService.generatePortalToken(
            account.getPatient().getId(), account.getEmail(), account.getPatient().getHospitalId());

    return PortalLoginResponse.builder()
        .token(token)
        .patientId(account.getPatient().getId())
        .patientName(account.getPatient().getFullName())
        .email(account.getEmail())
        .build();
  }

  // ------------------------------------------------
  // Dashboard do paciente
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public PortalDashboardDto getDashboard(UUID patientId) {
    Patient patient =
        patientRepository
            .findById(patientId)
            .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado"));

    // Episódios recentes
    List<PortalEpisodeDto> recentEpisodes =
        episodeRepository
            .findByPatientIdOrderByCreatedAtDesc(patientId, PageRequest.of(0, 5))
            .stream()
            .map(this::toPortalEpisodeDto)
            .toList();

    // Consultas futuras
    OffsetDateTime now = OffsetDateTime.now();

    List<PortalAppointmentDto> upcoming =
        appointmentRepository
            .findUpcomingByPatient(
                patientId, now.toLocalDate(), now.toLocalTime(), PageRequest.of(0, 5))
            .stream()
            .map(
                a ->
                    PortalAppointmentDto.builder()
                        .id(a.getId())
                        .doctorName(a.getDoctor() != null ? a.getDoctor().getFullName() : "")
                        .scheduledAt(
                            OffsetDateTime.of(
                                a.getAppointmentDate(), a.getStartTime(), now.getOffset()))
                        .status(a.getStatus().name())
                        .reason(a.getReason())
                        .build())
            .toList();

    var invoiceSummary = invoiceApplicationService.getSummary(patientId);

    return PortalDashboardDto.builder()
        .patientName(patient.getFullName())
        .totalEpisodes((int) episodeRepository.countByPatientId(patientId))
        .upcomingAppointments(upcoming.size())
        .pendingLabResults((int) labResultApplicationService.countPendingResults(patientId))
        .pendingInvoices((int) invoiceSummary.pendingInvoices())
        .totalDebt(invoiceSummary.totalDebt())
        .recentEpisodes(recentEpisodes)
        .upcomingAppointmentsList(upcoming)
        .build();
  }

  // ------------------------------------------------
  // Episódios / consultas
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public List<PortalEpisodeDto> getEpisodes(UUID patientId) {
    return episodeRepository
        .findByPatientIdOrderByCreatedAtDesc(patientId, PageRequest.of(0, 50))
        .stream()
        .map(this::toPortalEpisodeDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PortalLabResultDto> getLabResults(UUID patientId) {
    return labResultApplicationService.findCompletedResults(patientId).stream()
        .map(
            result ->
                PortalLabResultDto.builder()
                    .id(result.id())
                    .examName(result.examName())
                    .status(result.status())
                    .result(result.result())
                    .referenceValues(result.referenceValues())
                    .doctorName(result.doctorName())
                    .requestedAt(result.requestedAt())
                    .resultAt(result.resultAt())
                    .build())
        .toList();
  }

  // ------------------------------------------------
  // Prescrições
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public List<PortalPrescriptionDto> getPrescriptions(UUID patientId) {
    return prescriptionRepository
        .findByPatientIdOrderByPrescriptionDateDesc(patientId, PageRequest.of(0, 50))
        .stream()
        .map(
            p ->
                PortalPrescriptionDto.builder()
                    .id(p.getId())
                    .prescriptionNumber(p.getPrescriptionNumber())
                    .doctorName(p.getDoctor().getFullName())
                    .prescriptionDate(p.getPrescriptionDate())
                    .expiryDate(p.getExpiryDate())
                    .status(p.getStatus().name())
                    .statusLabel(p.getStatus().name())
                    .diagnosis(p.getDiagnosis())
                    .items(
                        p.getItems().stream()
                            .map(
                                i ->
                                    PortalPrescriptionItemDto.builder()
                                        .medicationName(i.getMedication().getName())
                                        .dosage(i.getDosage())
                                        .route(i.getRoute())
                                        .quantityPrescribed(i.getQuantityPrescribed())
                                        .quantityDispensed(i.getQuantityDispensed())
                                        .status(i.getStatus().name())
                                        .build())
                            .toList())
                    .build())
        .toList();
  }

  // ------------------------------------------------
  // Facturas
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public List<PortalInvoiceDto> getInvoices(UUID patientId) {
    return invoiceApplicationService.findVisibleInvoices(patientId).stream()
        .map(
            invoice ->
                PortalInvoiceDto.builder()
                    .id(invoice.id())
                    .invoiceNumber(invoice.invoiceNumber())
                    .issueDate(invoice.issueDate())
                    .totalAmount(invoice.totalAmount())
                    .status(invoice.status())
                    .statusLabel(invoice.statusLabel())
                    .build())
        .toList();
  }

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  private PortalEpisodeDto toPortalEpisodeDto(ao.hospitalao.modules.episodes.entity.Episode e) {
    return PortalEpisodeDto.builder()
        .id(e.getId())
        .episodeType(e.getEpisodeType().name())
        .status(e.getStatus().name())
        .statusLabel(e.getStatus().name())
        .doctorName(e.getDoctor() != null ? e.getDoctor().getFullName() : "")
        .scheduledAt(e.getScheduledAt())
        .completedAt(e.getCompletedAt())
        .reason(e.getReason())
        .diagnosis(e.getDiagnosis())
        .build();
  }

  private UUID requireCurrentHospital() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    if (hospitalId == null) {
      throw new IllegalStateException("A aprovação exige o âmbito de um hospital.");
    }
    return hospitalId;
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
