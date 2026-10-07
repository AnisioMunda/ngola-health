package ao.hospitalao.modules.telemedicine.service;
 
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.telemedicine.dto.TelemedicineDtos.*;
import ao.hospitalao.modules.telemedicine.entity.TelemedicineSession;
import ao.hospitalao.modules.telemedicine.entity.TelemedicineSession.SessionStatus;
import ao.hospitalao.modules.telemedicine.repository.TelemedicineRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
 
@Slf4j
@Service
@RequiredArgsConstructor
public class TelemedicineService {
 
    private final TelemedicineRepository telemedicineRepository;
    private final PatientRepository      patientRepository;
    private final UserRepository         userRepository;
    private final HospitalRepository     hospitalRepository;
 
    @Value("${app.base-url:https://hospitalao.ao}")
    private String baseUrl;
 
    // ------------------------------------------------
    // Stats
    // ------------------------------------------------
 
    @Transactional(readOnly = true)
    public TelemedicineStatsDto getStats() {
        UUID hospitalId = TenantContext.getCurrentHospital();
        return TelemedicineStatsDto.builder()
            .totalScheduled(telemedicineRepository.countByHospitalIdAndStatus(
                hospitalId, SessionStatus.SCHEDULED))
            .inProgress(telemedicineRepository.countByHospitalIdAndStatus(
                hospitalId, SessionStatus.IN_PROGRESS))
            .build();
    }
 
    // ------------------------------------------------
    // Sessões activas
    // ------------------------------------------------
 
    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions() {
        UUID hospitalId = TenantContext.getCurrentHospital();
        return telemedicineRepository.findActiveByHospital(hospitalId)
            .stream().map(this::toResponse).toList();
    }
 
    @Transactional(readOnly = true)
    public List<SessionResponse> getMySessionsToday() {
        var doctor = getCurrentUser();
        return telemedicineRepository
            .findByDoctorAndDate(doctor.getId(), OffsetDateTime.now().withHour(0))
            .stream().map(this::toResponse).toList();
    }
 
    // ------------------------------------------------
    // Criar sessão
    // ------------------------------------------------
 
    @Transactional
    public SessionResponse create(CreateSessionRequest req) {
        UUID hospitalId = TenantContext.getCurrentHospital();
        var doctor  = getCurrentUser();
        String token = UUID.randomUUID().toString().replace("-", "") +
            UUID.randomUUID().toString().replace("-", "");
 
        TelemedicineSession session = TelemedicineSession.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .patient(patientRepository.getReferenceById(req.getPatientId()))
            .doctor(doctor)
            .scheduledAt(req.getScheduledAt())
            .roomToken(token)
            .build();
 
        if (req.getAppointmentId() != null) {
            session.setAppointment(new ao.hospitalao.modules.scheduling.entity.Appointment(req.getAppointmentId()));
        }
 
        TelemedicineSession saved = telemedicineRepository.save(session);
        log.info("Telemedicine session created for patient {} with doctor {}",
            req.getPatientId(), doctor.getId());
        return toResponse(saved);
    }
 
    // ------------------------------------------------
    // Entrar na sala (inicia a sessão)
    // ------------------------------------------------
 
    @Transactional
    public SessionResponse joinSession(String roomToken) {
        TelemedicineSession session = telemedicineRepository
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
        TelemedicineSession session = telemedicineRepository
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
        session.setStatus(SessionStatus.CANCELLED);
        return toResponse(telemedicineRepository.save(session));
    }
 
    // ------------------------------------------------
    // Helpers
    // ------------------------------------------------
 
    private TelemedicineSession getOrThrow(UUID id) {
        return telemedicineRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Sessão não encontrada"));
    }
 
    private ao.hospitalao.modules.auth.entity.User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }
 
    private SessionResponse toResponse(TelemedicineSession s) {
        String roomUrl = baseUrl + "/video/" + s.getRoomToken();
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
            .roomUrl(roomUrl)
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
            case SCHEDULED   -> "Agendada";
            case WAITING     -> "Paciente em espera";
            case IN_PROGRESS -> "Em curso";
            case COMPLETED   -> "Concluída";
            case CANCELLED   -> "Cancelada";
            case NO_SHOW     -> "Não compareceu";
        };
    }
}