package ao.hospitalao.modules.telemedicine.service;

import ao.hospitalao.modules.auth.application.UserApplicationService;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.scheduling.application.TelemedicineAppointmentApplicationService;
import ao.hospitalao.modules.telemedicine.dto.TelemedicineDtos.*;
import ao.hospitalao.modules.telemedicine.entity.TelemedicineSession;
import ao.hospitalao.modules.telemedicine.entity.TelemedicineSession.SessionStatus;
import ao.hospitalao.modules.telemedicine.provider.TeamsMeetingProvider;
import ao.hospitalao.modules.telemedicine.provider.TeamsMeetingProviderException;
import ao.hospitalao.modules.telemedicine.repository.TelemedicineRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemedicineService {

  private final TelemedicineRepository telemedicineRepository;
  private final PatientRepository patientRepository;
  private final UserRepository userRepository;
  private final HospitalRepository hospitalRepository;
  private final TeamsMeetingProvider teamsMeetingProvider;
  private final UserApplicationService userApplicationService;
  private final TelemedicineAppointmentApplicationService appointmentApplicationService;

  // ------------------------------------------------
  // Stats
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public TelemedicineStatsDto getStats() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return TelemedicineStatsDto.builder()
        .totalScheduled(
            telemedicineRepository.countByHospitalIdAndStatus(hospitalId, SessionStatus.SCHEDULED))
        .inProgress(
            telemedicineRepository.countByHospitalIdAndStatus(
                hospitalId, SessionStatus.IN_PROGRESS))
        .teamsConfigured(teamsMeetingProvider.isConfigured())
        .build();
  }

  // ------------------------------------------------
  // Sessões activas
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public List<SessionResponse> getActiveSessions() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return telemedicineRepository.findActiveByHospital(hospitalId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<SessionResponse> getMySessionsToday() {
    var doctor = getCurrentUser();
    return telemedicineRepository
        .findByDoctorAndDate(doctor.getId(), OffsetDateTime.now().withHour(0))
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<DoctorOptionDto> getTeamsDoctors() {
    return userApplicationService.findTeamsDoctors(TenantContext.getCurrentHospital()).stream()
        .map(
            doctor -> DoctorOptionDto.builder().id(doctor.id()).fullName(doctor.fullName()).build())
        .toList();
  }

  // ------------------------------------------------
  // Criar sessão
  // ------------------------------------------------

  @Transactional
  public SessionResponse create(CreateSessionRequest req) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    if (hospitalId == null) {
      throw new AccessDeniedException("É necessário um hospital autenticado para criar a sessão.");
    }
    var patient = patientRepository.getReferenceById(req.getPatientId());
    if (!hospitalId.equals(patient.getHospitalId())) {
      throw new AccessDeniedException("O paciente pertence a outro hospital.");
    }
    var organizer = userApplicationService.findTeamsDoctor(req.getDoctorId(), hospitalId);
    if (req.getAppointmentId() != null) {
      appointmentApplicationService.validateForTelemedicine(
          req.getAppointmentId(), hospitalId, req.getPatientId(), req.getDoctorId());
    }
    UUID providerOrganizerId = UUID.fromString(organizer.organizerId());
    var meeting =
        teamsMeetingProvider.createMeeting(
            req.getScheduledAt(), req.getDurationMinutes(), organizer.organizerId());
    String token =
        UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", "");

    TelemedicineSession session =
        TelemedicineSession.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .patient(patient)
            .doctor(userRepository.getReferenceById(organizer.id()))
            .scheduledAt(req.getScheduledAt())
            .roomToken(token)
            .providerMeetingId(meeting.id())
            .providerOrganizerId(providerOrganizerId)
            .roomUrl(meeting.joinUrl())
            .durationMinutes(req.getDurationMinutes())
            .build();

    session.setAppointmentId(req.getAppointmentId());

    TelemedicineSession saved;
    try {
      saved = telemedicineRepository.saveAndFlush(session);
    } catch (DataAccessException persistenceException) {
      try {
        teamsMeetingProvider.deleteMeeting(meeting.id(), organizer.organizerId());
      } catch (TeamsMeetingProviderException | IllegalStateException cleanupException) {
        persistenceException.addSuppressed(cleanupException);
        log.error("Failed to revoke an unpersisted Microsoft Teams meeting", cleanupException);
      }
      throw persistenceException;
    }
    log.info("Telemedicine session created");
    return toResponse(saved);
  }

  // ------------------------------------------------
  // Entrar na sala (inicia a sessão)
  // ------------------------------------------------

  @Transactional
  public SessionResponse joinSession(String roomToken) {
    TelemedicineSession session =
        telemedicineRepository
            .findByRoomToken(roomToken)
            .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada."));

    if (session.getStatus() == SessionStatus.SCHEDULED
        || session.getStatus() == SessionStatus.WAITING) {
      session.setStatus(SessionStatus.IN_PROGRESS);
      session.setStartedAt(OffsetDateTime.now());
      telemedicineRepository.save(session);
    }
    return toResponse(session);
  }

  // ------------------------------------------------
  // Entrar na sala de espera
  // ------------------------------------------------

  @Transactional
  public SessionResponse patientWaiting(String roomToken) {
    TelemedicineSession session =
        telemedicineRepository
            .findByRoomToken(roomToken)
            .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada."));

    if (session.getStatus() == SessionStatus.SCHEDULED) {
      session.setStatus(SessionStatus.WAITING);
      telemedicineRepository.save(session);
    }
    return toResponse(session);
  }

  // ------------------------------------------------
  // Terminar sessão
  // ------------------------------------------------

  @Transactional
  public SessionResponse endSession(UUID id, UpdateNotesRequest req) {
    TelemedicineSession session = getOrThrow(id);
    revokeTeamsMeeting(session);
    session.setStatus(SessionStatus.COMPLETED);
    session.setEndedAt(OffsetDateTime.now());
    session.setClinicalNotes(req.getClinicalNotes());
    session.calculateDuration();
    return toResponse(telemedicineRepository.save(session));
  }

  // ------------------------------------------------
  // Cancelar
  // ------------------------------------------------

  @Transactional
  public SessionResponse cancel(UUID id) {
    TelemedicineSession session = getOrThrow(id);
    if (session.getStatus() == SessionStatus.COMPLETED) {
      throw new IllegalStateException("Sessão já concluída.");
    }
    revokeTeamsMeeting(session);
    session.setStatus(SessionStatus.CANCELLED);
    return toResponse(telemedicineRepository.save(session));
  }

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  private TelemedicineSession getOrThrow(UUID id) {
    return telemedicineRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Sessão não encontrada"));
  }

  private void revokeTeamsMeeting(TelemedicineSession session) {
    if (session.getProviderMeetingId() == null) {
      return;
    }
    if (session.getProviderOrganizerId() == null) {
      throw new IllegalStateException(
          "A reunião Teams não contém o organizador necessário para revogar a ligação.");
    }
    teamsMeetingProvider.deleteMeeting(
        session.getProviderMeetingId(), session.getProviderOrganizerId().toString());
    session.setRoomUrl(null);
  }

  private ao.hospitalao.modules.auth.entity.User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username).orElseThrow();
  }

  private SessionResponse toResponse(TelemedicineSession s) {
    return SessionResponse.builder()
        .id(s.getId())
        .patientId(s.getPatient().getId())
        .patientName(s.getPatient().getFullName())
        .doctorId(s.getDoctor().getId())
        .doctorName(s.getDoctor().getFullName())
        .appointmentId(s.getAppointment() != null ? s.getAppointment().getId() : null)
        .status(s.getStatus())
        .statusLabel(statusLabel(s.getStatus()))
        .roomToken(s.getRoomToken())
        .roomUrl(s.getRoomUrl())
        .scheduledAt(s.getScheduledAt())
        .startedAt(s.getStartedAt())
        .endedAt(s.getEndedAt())
        .durationMinutes(s.getDurationMinutes())
        .clinicalNotes(s.getClinicalNotes())
        .createdAt(s.getCreatedAt())
        .build();
  }

  private String statusLabel(SessionStatus s) {
    return switch (s) {
      case SCHEDULED -> "Agendada";
      case WAITING -> "Paciente em espera";
      case IN_PROGRESS -> "Em curso";
      case COMPLETED -> "Concluída";
      case CANCELLED -> "Cancelada";
      case NO_SHOW -> "Não compareceu";
    };
  }
}
