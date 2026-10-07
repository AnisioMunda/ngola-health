package ao.hospitalao.modules.scheduling.service;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
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
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    UUID hospitalId = TenantContext.getCurrentHospital();
    var doctor =
        userRepository
            .findById(req.getDoctorId())
            .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

    DoctorSchedule schedule =
        DoctorSchedule.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .doctor(doctor)
            .dayOfWeek(req.getDayOfWeek())
            .startTime(req.getStartTime())
            .endTime(req.getEndTime())
            .slotDurationMinutes(
                req.getSlotDurationMinutes() > 0 ? req.getSlotDurationMinutes() : 30)
            .maxPatientsPerSlot(req.getMaxPatientsPerSlot() > 0 ? req.getMaxPatientsPerSlot() : 1)
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
                              && !slotStart.isBefore(b.getStartTime())
                              && !slotStart.isAfter(b.getEndTime()));

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
    UUID hospitalId = TenantContext.getCurrentHospital();

    // Verificar conflito
    if (appointmentRepository.existsConflict(
        req.getDoctorId(), req.getAppointmentDate(), req.getStartTime(), null)) {
      throw new IllegalStateException(
          "Slot indisponível. Já existe marcação para este médico neste horário.");
    }

    var patient =
        patientRepository
            .findById(req.getPatientId())
            .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado"));
    var doctor =
        userRepository
            .findById(req.getDoctorId())
            .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

    // Calcular hora de fim com base no horário do médico
    int dow = req.getAppointmentDate().getDayOfWeek().getValue() - 1;
    int duration =
        scheduleRepository
            .findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(req.getDoctorId())
            .stream()
            .filter(s -> s.getDayOfWeek() == dow)
            .findFirst()
            .map(DoctorSchedule::getSlotDurationMinutes)
            .orElse(30);

    Appointment appointment =
        Appointment.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .patient(patient)
            .doctor(doctor)
            .appointmentDate(req.getAppointmentDate())
            .startTime(req.getStartTime())
            .endTime(req.getStartTime().plusMinutes(duration))
            .appointmentType(
                req.getAppointmentType() != null
                    ? req.getAppointmentType()
                    : Appointment.AppointmentType.OUTPATIENT)
            .reason(req.getReason())
            .notes(req.getNotes())
            .status(AppointmentStatus.SCHEDULED)
            .bookedBy(getCurrentUser())
            .build();

    Appointment saved = appointmentRepository.save(appointment);
    log.info(
        "Appointment created: {} {} {} for patient {}",
        saved.getAppointmentDate(),
        saved.getStartTime(),
        doctor.getFullName(),
        patient.getFullName());
    return toAppointmentResponse(saved);
  }

  @Transactional
  public AppointmentResponse confirm(UUID id) {
    Appointment a = getOrThrow(id);
    if (a.getStatus() != AppointmentStatus.SCHEDULED) {
      throw new IllegalStateException("Apenas agendamentos SCHEDULED podem ser confirmados.");
    }
    a.setStatus(AppointmentStatus.CONFIRMED);
    a.setConfirmedAt(OffsetDateTime.now());
    return toAppointmentResponse(appointmentRepository.save(a));
  }

  @Transactional
  public AppointmentResponse complete(UUID id) {
    Appointment a = getOrThrow(id);
    if (a.getStatus() == AppointmentStatus.CANCELLED
        || a.getStatus() == AppointmentStatus.NO_SHOW) {
      throw new IllegalStateException("Não é possível completar este agendamento.");
    }
    a.setStatus(AppointmentStatus.COMPLETED);
    return toAppointmentResponse(appointmentRepository.save(a));
  }

  @Transactional
  public AppointmentResponse cancel(UUID id, String reason) {
    Appointment a = getOrThrow(id);
    if (a.getStatus() == AppointmentStatus.COMPLETED) {
      throw new IllegalStateException("Não é possível cancelar uma consulta já realizada.");
    }
    a.setStatus(AppointmentStatus.CANCELLED);
    a.setCancellationReason(reason);
    a.setCancelledAt(OffsetDateTime.now());
    log.info("Appointment {} cancelled: {}", a.getId(), reason);
    return toAppointmentResponse(appointmentRepository.save(a));
  }

  @Transactional
  public AppointmentResponse noShow(UUID id) {
    Appointment a = getOrThrow(id);
    a.setStatus(AppointmentStatus.NO_SHOW);
    return toAppointmentResponse(appointmentRepository.save(a));
  }

  // ------------------------------------------------
  // Bloqueios de agenda
  // ------------------------------------------------

  @Transactional
  public void createBlock(CreateBlockRequest req) {
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

  private ao.hospitalao.modules.auth.entity.User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username).orElse(null);
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
