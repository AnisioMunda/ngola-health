package ao.hospitalao.modules.portal.service;

import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import ao.hospitalao.modules.laboratory.repository.LabRequestRepository;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.portal.dto.PortalDtos.*;
import ao.hospitalao.modules.portal.entity.PatientPortalAccount;
import ao.hospitalao.modules.portal.repository.PatientPortalAccountRepository;
import ao.hospitalao.modules.prescription.repository.PrescriptionRepository;
import ao.hospitalao.modules.scheduling.repository.AppointmentRepository;
import ao.hospitalao.security.jwt.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientPortalService {

    private final PatientPortalAccountRepository portalAccountRepository;
    private final PatientRepository              patientRepository;
    private final EpisodeRepository              episodeRepository;
    private final AppointmentRepository          appointmentRepository;
    private final LabRequestRepository           labRequestRepository;
    private final InvoiceRepository              invoiceRepository;
    private final PrescriptionRepository         prescriptionRepository;
    private final PasswordEncoder                passwordEncoder;
    private final JwtService                     jwtService;

    // ------------------------------------------------
    // Registo
    // ------------------------------------------------

    @Transactional
    public PortalLoginResponse register(PortalRegisterRequest req) {
        // Encontrar paciente por NIF ou número de processo
        Patient patient = patientRepository
        .findByNifOrPatientNumber(req.getPatientNumber())
        .orElseThrow(() -> new EntityNotFoundException(
            "Paciente não encontrado com o número: "
                + req.getPatientNumber()
                + ". Contacte a recepção."
        ));

        if (portalAccountRepository.existsByPatientId(patient.getId())) {
            throw new IllegalStateException(
                "Já existe uma conta portal para este paciente.");
        }
        if (portalAccountRepository.existsByEmail(req.getEmail())) {
            throw new IllegalStateException("Este email já está registado.");
        }

        PatientPortalAccount account = PatientPortalAccount.builder()
            .patient(patient)
            .email(req.getEmail())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .build();

        portalAccountRepository.save(account);
        log.info("Portal account created for patient {}", patient.getId());

        String token = jwtService.generatePortalToken(patient.getId(), req.getEmail());
        return PortalLoginResponse.builder()
            .token(token)
            .patientId(patient.getId())
            .patientName(patient.getFullName())
            .email(req.getEmail())
            .build();
    }

    // ------------------------------------------------
    // Login
    // ------------------------------------------------

    @Transactional
    public PortalLoginResponse login(PortalLoginRequest req) {
        PatientPortalAccount account = portalAccountRepository
            .findByEmail(req.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Email ou password incorrectos."));

        if (!account.isActive()) {
            throw new IllegalStateException("Conta desactivada. Contacte o hospital.");
        }
        if (!passwordEncoder.matches(req.getPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Email ou password incorrectos.");
        }

        account.setLastLoginAt(OffsetDateTime.now());
        portalAccountRepository.save(account);

        String token = jwtService.generatePortalToken(
            account.getPatient().getId(), account.getEmail());

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
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado"));

        // Episódios recentes
        List<PortalEpisodeDto> recentEpisodes = episodeRepository
            .findByPatientIdOrderByCreatedAtDesc(patientId, PageRequest.of(0, 5))
            .stream().map(this::toPortalEpisodeDto).toList();

        // Consultas futuras
        OffsetDateTime now = OffsetDateTime.now();

        List<PortalAppointmentDto> upcoming = appointmentRepository
            .findUpcomingByPatient(
                patientId,
                now.toLocalDate(),
                now.toLocalTime(),
                PageRequest.of(0, 5)
            )
            .stream()
            .map(a -> PortalAppointmentDto.builder()
                .id(a.getId())
                .doctorName(
                    a.getDoctor() != null
                        ? a.getDoctor().getFullName()
                        : ""
                )
                .scheduledAt(
                    OffsetDateTime.of(
                        a.getAppointmentDate(),
                        a.getStartTime(),
                        now.getOffset()
                    )
                )
                .status(a.getStatus().name())
                .reason(a.getReason())
                .build()
            )
            .toList();

        return PortalDashboardDto.builder()
            .patientName(patient.getFullName())
            .totalEpisodes((int) episodeRepository.countByPatientId(patientId))
            .upcomingAppointments(upcoming.size())
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
            .stream().map(this::toPortalEpisodeDto).toList();
    }

    // ------------------------------------------------
    // Prescrições
    // ------------------------------------------------

    @Transactional(readOnly = true)
    public List<PortalPrescriptionDto> getPrescriptions(UUID patientId) {
        return prescriptionRepository
            .findByPatientIdOrderByPrescriptionDateDesc(patientId, PageRequest.of(0, 50))
            .stream().map(p -> PortalPrescriptionDto.builder()
                .id(p.getId())
                .prescriptionNumber(p.getPrescriptionNumber())
                .doctorName(p.getDoctor().getFullName())
                .prescriptionDate(p.getPrescriptionDate())
                .expiryDate(p.getExpiryDate())
                .status(p.getStatus().name())
                .statusLabel(p.getStatus().name())
                .diagnosis(p.getDiagnosis())
                .items(p.getItems().stream().map(i -> PortalPrescriptionItemDto.builder()
                    .medicationName(i.getMedication().getName())
                    .dosage(i.getDosage())
                    .route(i.getRoute())
                    .quantityPrescribed(i.getQuantityPrescribed())
                    .quantityDispensed(i.getQuantityDispensed())
                    .status(i.getStatus().name())
                    .build()).toList())
                .build()).toList();
    }

    // ------------------------------------------------
    // Facturas
    // ------------------------------------------------

    @Transactional(readOnly = true)
    public List<PortalInvoiceDto> getInvoices(UUID patientId) {

        List<Invoice> invoices =
            invoiceRepository.findByPatientIdOrderByIssuedAtDesc(
                patientId,
                PageRequest.of(0, 50)
            );

        return invoices.stream()
            .map(invoice -> {

                PortalInvoiceDto dto = PortalInvoiceDto.builder()
                    .id(invoice.getId())
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .issueDate(invoice.getIssuedAt())
                    .totalAmount(invoice.getTotalAmount())
                    .status(invoice.getStatus().toString())
                    .statusLabel(invoice.getStatus().toString())
                    .build();
                return dto;
        }).toList();
    }

    // ------------------------------------------------
    // Helpers
    // ------------------------------------------------

    private PortalEpisodeDto toPortalEpisodeDto(
        ao.hospitalao.modules.episodes.entity.Episode e) {
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
}