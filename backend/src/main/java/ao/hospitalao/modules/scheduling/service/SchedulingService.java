package ao.hospitalao.modules.scheduling.service;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.notifications.event.AppointmentCancelledEvent;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.scheduling.dto.SchedulingDtos.*;
import ao.hospitalao.modules.scheduling.entity.Appointment;
import ao.hospitalao.modules.scheduling.entity.Appointment.AppointmentStatus;
import ao.hospitalao.modules.scheduling.entity.DoctorSchedule;
import ao.hospitalao.modules.scheduling.entity.ScheduleBlock;
import ao.hospitalao.modules.scheduling.repository.AppointmentRepository;
import ao.hospitalao.modules.scheduling.repository.DoctorScheduleRepository;
import ao.hospitalao.modules.scheduling.repository.ScheduleBlockRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingService {

  private final AppointmentRepository appointmentRepository;
  private final DoctorScheduleRepository scheduleRepository;
  private final ScheduleBlockRepository blockRepository;
  private final PatientRepository patientRepository;
  private final UserRepository userRepository;
  private final HospitalRepository hospitalRepository;
  private final ApplicationEventPublisher eventPublisher;

  private static final Locale PT = new Locale("pt", "AO");
  private static final DateTimeFormatter DATE_LABEL =
      DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", PT);
  private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT);

  // ------------------------------------------------
  // Horários dos médicos
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public List<DoctorScheduleResponse> getDoctorSchedules(UUID doctorId) {
    return scheduleRepository
        .findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(doctorId)
        .stream()
        .map(this::toScheduleResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<DoctorScheduleResponse> getHospitalSchedules() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return scheduleRepository
        .findByHospitalIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(hospitalId)
        .stream()
        .map(this::toScheduleResponse)
        .toList();
  }

  @Transactional
  public DoctorScheduleResponse createSchedule(CreateScheduleRequest req) {
    if (req.getDoctorId() == null
        || req.getDayOfWeek() == null
        || req.getDayOfWeek() < 0
        || req.getDayOfWeek() > 6
        || req.getStartTime() == null
        || req.getEndTime() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Médico, dia da semana e horas são obrigatórios.");
    }
    if (!req.getStartTime().isBefore(req.getEndTime())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A hora de início deve ser anterior à hora de fim.");
    }
    if ((req.getSlotDurationMinutes() != null && req.getSlotDurationMinutes() < 1)
        || (req.getMaxPatientsPerSlot() != null && req.getMaxPatientsPerSlot() < 1)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A duração e a capacidade dos slots devem ser positivas.");
    }
    int slotDurationMinutes =
        req.getSlotDurationMinutes() != null ? req.getSlotDurationMinutes() : 30;
    if (slotDurationMinutes > ChronoUnit.MINUTES.between(req.getStartTime(), req.getEndTime())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A duração do slot não pode exceder o horário definido.");
    }

    UUID hospitalId = TenantContext.getCurrentHospital();
    var doctor =
        userRepository
            .findById(req.getDoctorId())
            .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

    if (scheduleRepository.existsOverlappingSchedule(
        req.getDoctorId(), req.getDayOfWeek(), req.getStartTime(), req.getEndTime())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O horário sobrepõe-se a outro horário activo do médico.");
    }

    DoctorSchedule schedule =
        DoctorSchedule.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .doctor(doctor)
            .dayOfWeek(req.getDayOfWeek())
            .startTime(req.getStartTime())
            .endTime(req.getEndTime())
            .slotDurationMinutes(slotDurationMinutes)
            .maxPatientsPerSlot(
                req.getMaxPatientsPerSlot() != null ? req.getMaxPatientsPerSlot() : 1)
            .build();

    return toScheduleResponse(scheduleRepository.save(schedule));
  }

  @Transactional
  public void deleteSchedule(UUID id) {
    DoctorSchedule s =
        scheduleRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Horário não encontrado"));
    s.setActive(false);
    scheduleRepository.save(s);
  }

  // ------------------------------------------------
  // Slots disponíveis
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public DayAvailabilityResponse getAvailability(UUID doctorId, LocalDate date) {
    // Dia da semana: Java DayOfWeek começa em 1 (Monday),
    // nosso modelo: 0=Segunda, ..., 6=Domingo
    int dow = date.getDayOfWeek().getValue() - 1;

    // Horários do médico para este dia
    List<DoctorSchedule> schedules =
        scheduleRepository
            .findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(doctorId)
            .stream()
            .filter(s -> s.getDayOfWeek() == dow)
            .toList();

    // Bloqueios do médico neste dia
    List<ScheduleBlock> blocks = blockRepository.findByDoctorAndDate(doctorId, date);

    boolean fullyBlocked = blocks.stream().anyMatch(ScheduleBlock::isAllDay);

    // Agendamentos já existentes
    List<Appointment> existing =
        appointmentRepository.findByDoctorIdAndAppointmentDateOrderByStartTime(doctorId, date);

    List<SlotResponse> slots = new ArrayList<>();

    if (!fullyBlocked) {
      for (DoctorSchedule schedule : schedules) {
        LocalTime current = schedule.getStartTime();
        while (current
                .plusMinutes(schedule.getSlotDurationMinutes())
                .compareTo(schedule.getEndTime())
            <= 0) {

          final LocalTime slotStart = current;
          final LocalTime slotEnd = current.plusMinutes(schedule.getSlotDurationMinutes());

          // Verificar bloqueio parcial
          boolean partiallyBlocked =
              blocks.stream()
                  .filter(b -> !b.isAllDay())
                  .anyMatch(
                      b ->
                          b.getStartTime() != null
                              && b.getEndTime() != null
                              && slotStart.isBefore(b.getEndTime())
                              && slotEnd.isAfter(b.getStartTime()));

          // Contar marcações neste slot
          long booked =
              existing.stream()
                  .filter(
                      a ->
                          a.getStartTime().equals(slotStart)
                              && a.getStatus() != AppointmentStatus.CANCELLED
                              && a.getStatus() != AppointmentStatus.NO_SHOW)
                  .count();

          boolean available =
              !partiallyBlocked
                  && booked < schedule.getMaxPatientsPerSlot()
                  && !date.isBefore(LocalDate.now());

          slots.add(
              SlotResponse.builder()
                  .date(date)
                  .startTime(slotStart)
                  .endTime(slotEnd)
                  .doctorId(doctorId)
                  .doctorName(schedule.getDoctor().getFullName())
                  .available(available)
                  .bookedCount((int) booked)
                  .maxPatients(schedule.getMaxPatientsPerSlot())
                  .build());

          current = slotEnd;
        }
      }
    }

    long availableCount = slots.stream().filter(SlotResponse::isAvailable).count();

    return DayAvailabilityResponse.builder()
        .date(date)
        .dateLabel(date.format(DATE_LABEL))
        .dayOfWeek(date.getDayOfWeek().getDisplayName(TextStyle.FULL, PT))
        .slots(slots)
        .totalSlots(slots.size())
        .availableSlots((int) availableCount)
        .build();
  }

  // ------------------------------------------------
  // Agendamentos
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public Page<AppointmentResponse> findAll(
      UUID doctorId, UUID patientId, AppointmentStatus status, LocalDate date, Pageable pageable) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return appointmentRepository
        .findWithFilters(hospitalId, doctorId, patientId, status, date, pageable)
        .map(this::toAppointmentResponse);
  }

  @Transactional(readOnly = true)
  public AppointmentResponse findById(UUID id) {
    return appointmentRepository
        .findById(id)
        .map(this::toAppointmentResponse)
        .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado: " + id));
  }

  @Transactional(readOnly = true)
  public List<CalendarEventResponse> getCalendarEvents(LocalDate from, LocalDate to) {
    UUID hospitalId = TenantContext.getCurrentHospital();
    return appointmentRepository.findByHospitalAndDateRange(hospitalId, from, to).stream()
        .map(this::toCalendarEvent)
        .toList();
  }

  @Transactional
  public AppointmentResponse create(CreateAppointmentRequest req) {
    if (req.getPatientId() == null
        || req.getDoctorId() == null
        || req.getAppointmentDate() == null
        || req.getStartTime() == null
        || req.getReason() == null
        || req.getReason().isBlank()
        || req.getReason().length() > 300) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Paciente, médico, data, hora e motivo são obrigatórios.");
    }
    if (req.getStartTime().getSecond() != 0 || req.getStartTime().getNano() != 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A hora da marcação deve corresponder a um slot válido.");
    }
    if (req.getAppointmentDate().isBefore(LocalDate.now())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Não é possível marcar consultas em datas passadas.");
    }

    UUID hospitalId = TenantContext.getCurrentHospital();
    var patient =
        patientRepository
            .findById(req.getPatientId())
            .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado"));
    var doctor =
        userRepository
            .findById(req.getDoctorId())
            .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

    int dow = req.getAppointmentDate().getDayOfWeek().getValue() - 1;
    DoctorSchedule schedule =
        scheduleRepository.findActiveSchedulesForBooking(req.getDoctorId(), dow).stream()
            .filter(s -> isValidSlotStart(s, req.getStartTime()))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT, "O horário solicitado não está disponível."));
    LocalTime slotEnd = req.getStartTime().plusMinutes(schedule.getSlotDurationMinutes());

    boolean blocked =
        blockRepository.findByDoctorAndDate(req.getDoctorId(), req.getAppointmentDate()).stream()
            .anyMatch(
                block ->
                    block.isAllDay()
                        || (block.getStartTime() != null
                            && block.getEndTime() != null
                            && req.getStartTime().isBefore(block.getEndTime())
                            && slotEnd.isAfter(block.getStartTime())));
    if (blocked) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O horário solicitado está bloqueado.");
    }

    if (appointmentRepository.existsActiveAppointmentForPatientInSlot(
        req.getPatientId(), req.getDoctorId(), req.getAppointmentDate(), req.getStartTime())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Este paciente já tem uma consulta marcada neste horário.");
    }

    Set<Integer> occupiedPositions =
        new HashSet<>(
            appointmentRepository.findOccupiedSlotPositions(
                req.getDoctorId(), req.getAppointmentDate(), req.getStartTime()));
    int capacity = schedule.getMaxPatientsPerSlot();
    if (capacity < 1 || occupiedPositions.size() >= capacity) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O horário solicitado atingiu a capacidade máxima.");
    }
    int slotPosition = 0;
    for (int candidate = 1; candidate > 0 && candidate <= capacity; candidate++) {
      if (!occupiedPositions.contains(candidate)) {
        slotPosition = candidate;
        break;
      }
    }
    if (slotPosition == 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O horário solicitado atingiu a capacidade máxima.");
    }

    Appointment appointment =
        Appointment.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .patient(patient)
            .doctor(doctor)
            .appointmentDate(req.getAppointmentDate())
            .startTime(req.getStartTime())
            .endTime(slotEnd)
            .slotPosition(slotPosition)
            .appointmentType(
                req.getAppointmentType() != null
                    ? req.getAppointmentType()
                    : Appointment.AppointmentType.OUTPATIENT)
            .reason(req.getReason())
            .notes(req.getNotes())
            .status(AppointmentStatus.SCHEDULED)
            .bookedBy(getCurrentUser())
            .build();

    Appointment saved;
    try {
      saved = appointmentRepository.saveAndFlush(appointment);
    } catch (DataIntegrityViolationException exception) {
      if (!isSlotPositionConstraintViolation(exception)) {
        throw exception;
      }
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O lugar deste horário acabou de ser reservado.", exception);
    }
    log.info(
        "Appointment created: {} {} {} for patient {}",
        saved.getAppointmentDate(),
        saved.getStartTime(),
        doctor.getFullName(),
        patient.getFullName());
    return toAppointmentResponse(saved);
  }

  private boolean isValidSlotStart(DoctorSchedule schedule, LocalTime startTime) {
    int duration = schedule.getSlotDurationMinutes();
    if (duration < 1 || startTime.isBefore(schedule.getStartTime())) {
      return false;
    }
    long offsetMinutes = ChronoUnit.MINUTES.between(schedule.getStartTime(), startTime);
    return offsetMinutes % duration == 0
        && !startTime.plusMinutes(duration).isAfter(schedule.getEndTime());
  }

  private boolean isSlotPositionConstraintViolation(Throwable exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        String constraintName = violation.getConstraintName();
        if ("uq_appointment_slot_position_active".equals(constraintName)
            || "uq_appointment_patient_slot_active".equals(constraintName)) {
          return true;
        }
      }
    }
    return false;
  }

  @Transactional
  public AppointmentResponse confirm(UUID id) {
    Appointment a = getOrThrow(id);
    if (a.getStatus() != AppointmentStatus.SCHEDULED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Apenas agendamentos SCHEDULED podem ser confirmados.");
    }
    a.setStatus(AppointmentStatus.CONFIRMED);
    a.setConfirmedAt(OffsetDateTime.now());
    return toAppointmentResponse(appointmentRepository.save(a));
  }

  @Transactional
  public AppointmentResponse complete(UUID id) {
    Appointment a = getOrThrow(id);
    if (a.getStatus() != AppointmentStatus.SCHEDULED
        && a.getStatus() != AppointmentStatus.CONFIRMED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Apenas consultas agendadas ou confirmadas podem ser concluídas.");
    }
    a.setStatus(AppointmentStatus.COMPLETED);
    return toAppointmentResponse(appointmentRepository.save(a));
  }

  @Transactional
  public AppointmentResponse cancel(UUID id, String reason) {
    Appointment a = getOrThrow(id);
    if (a.getStatus() == AppointmentStatus.COMPLETED
        || a.getStatus() == AppointmentStatus.NO_SHOW) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Consultas realizadas ou com falta não podem ser canceladas.");
    }
    boolean wasAlreadyCancelled = a.getStatus() == AppointmentStatus.CANCELLED;
    a.setStatus(AppointmentStatus.CANCELLED);
    a.setCancellationReason(reason);
    a.setCancelledAt(OffsetDateTime.now());
    log.info("Appointment {} cancelled: {}", a.getId(), reason);
    Appointment saved = appointmentRepository.save(a);
    if (!wasAlreadyCancelled) {
      eventPublisher.publishEvent(
          new AppointmentCancelledEvent(
              a.getHospitalId(),
              a.getId(),
              a.getDoctor().getId(),
              a.getPatient().getFullName(),
              a.getAppointmentDate(),
              a.getStartTime()));
    }
    return toAppointmentResponse(saved);
  }

  @Transactional
  public AppointmentResponse noShow(UUID id) {
    Appointment a = getOrThrow(id);
    if (a.getStatus() != AppointmentStatus.SCHEDULED
        && a.getStatus() != AppointmentStatus.CONFIRMED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "A falta só pode ser registada numa consulta agendada ou confirmada.");
    }
    a.setStatus(AppointmentStatus.NO_SHOW);
    return toAppointmentResponse(appointmentRepository.save(a));
  }

  // ------------------------------------------------
  // Bloqueios de agenda
  // ------------------------------------------------

  @Transactional
  public void createBlock(CreateBlockRequest req) {
    if (!req.isAllDay()
        && (req.getStartTime() == null
            || req.getEndTime() == null
            || !req.getStartTime().isBefore(req.getEndTime()))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Um bloqueio parcial exige horas válidas de início e fim.");
    }

    UUID hospitalId = TenantContext.getCurrentHospital();
    var doctor =
        userRepository
            .findById(req.getDoctorId())
            .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

    ScheduleBlock block =
        ScheduleBlock.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .doctor(doctor)
            .blockDate(req.getBlockDate())
            .startTime(req.getStartTime())
            .endTime(req.getEndTime())
            .allDay(req.isAllDay())
            .reason(req.getReason())
            .build();

    blockRepository.save(block);
    log.info(
        "Schedule block created for doctor {} on {}", doctor.getFullName(), req.getBlockDate());
  }

  // ------------------------------------------------
  // Helpers / Mappers
  // ------------------------------------------------

  private Appointment getOrThrow(UUID id) {
    return appointmentRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado: " + id));
  }

  private User getCurrentUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "É necessário autenticar o utilizador que cria a marcação.");
    }
    return userRepository
        .findByUsername(authentication.getName())
        .orElseThrow(() -> new EntityNotFoundException("Utilizador autenticado não encontrado"));
  }

  private DoctorScheduleResponse toScheduleResponse(DoctorSchedule s) {
    return DoctorScheduleResponse.builder()
        .id(s.getId())
        .doctorId(s.getDoctor().getId())
        .doctorName(s.getDoctor().getFullName())
        .specialty(s.getDoctor().getEspeciality())
        .dayOfWeek(s.getDayOfWeek())
        .dayLabel(DoctorSchedule.DayOfWeekLabel.label(s.getDayOfWeek()))
        .startTime(s.getStartTime().toString())
        .endTime(s.getEndTime().toString())
        .slotDurationMinutes(s.getSlotDurationMinutes())
        .maxPatientsPerSlot(s.getMaxPatientsPerSlot())
        .active(s.isActive())
        .build();
  }

  private AppointmentResponse toAppointmentResponse(Appointment a) {
    return AppointmentResponse.builder()
        .id(a.getId())
        .patientId(a.getPatient().getId())
        .patientName(a.getPatient().getFullName())
        .patientPhone(a.getPatient().getPhone())
        .doctorId(a.getDoctor().getId())
        .doctorName(a.getDoctor().getFullName())
        .doctorSpecialty(a.getDoctor().getEspeciality())
        .appointmentDate(a.getAppointmentDate())
        .appointmentDateLabel(a.getAppointmentDate().format(DATE_SHORT))
        .startTime(a.getStartTime())
        .endTime(a.getEndTime())
        .status(a.getStatus())
        .statusLabel(statusLabel(a.getStatus()))
        .appointmentType(a.getAppointmentType())
        .reason(a.getReason())
        .notes(a.getNotes())
        .cancellationReason(a.getCancellationReason())
        .bookedByName(a.getBookedBy() != null ? a.getBookedBy().getFullName() : null)
        .createdAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null)
        .confirmedAt(a.getConfirmedAt() != null ? a.getConfirmedAt().toString() : null)
        .build();
  }

  private CalendarEventResponse toCalendarEvent(Appointment a) {
    return CalendarEventResponse.builder()
        .id(a.getId())
        .doctorId(a.getDoctor().getId())
        .title(a.getPatient().getFullName())
        .date(a.getAppointmentDate())
        .startTime(a.getStartTime())
        .endTime(a.getEndTime())
        .patientName(a.getPatient().getFullName())
        .doctorName(a.getDoctor().getFullName())
        .status(a.getStatus())
        .color(statusColor(a.getStatus()))
        .build();
  }

  private String statusLabel(AppointmentStatus s) {
    return switch (s) {
      case SCHEDULED -> "Agendado";
      case CONFIRMED -> "Confirmado";
      case COMPLETED -> "Realizado";
      case CANCELLED -> "Cancelado";
      case NO_SHOW -> "Não Compareceu";
    };
  }

  private String statusColor(AppointmentStatus s) {
    return switch (s) {
      case SCHEDULED -> "#3b82f6";
      case CONFIRMED -> "#16a34a";
      case COMPLETED -> "#6b7280";
      case CANCELLED -> "#ef4444";
      case NO_SHOW -> "#f59e0b";
    };
  }
}
