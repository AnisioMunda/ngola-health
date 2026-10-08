package ao.hospitalao.modules.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.notifications.application.event.AppointmentCancelledEvent;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.scheduling.dto.SchedulingDtos.CalendarEventResponse;
import ao.hospitalao.modules.scheduling.dto.SchedulingDtos.CreateScheduleRequest;
import ao.hospitalao.modules.scheduling.entity.Appointment;
import ao.hospitalao.modules.scheduling.entity.Appointment.AppointmentStatus;
import ao.hospitalao.modules.scheduling.entity.Appointment.AppointmentType;
import ao.hospitalao.modules.scheduling.entity.DoctorSchedule;
import ao.hospitalao.modules.scheduling.entity.ScheduleBlock;
import ao.hospitalao.modules.scheduling.repository.AppointmentRepository;
import ao.hospitalao.modules.scheduling.repository.DoctorScheduleRepository;
import ao.hospitalao.modules.scheduling.repository.ScheduleBlockRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

  @Mock private AppointmentRepository appointmentRepository;
  @Mock private DoctorScheduleRepository scheduleRepository;
  @Mock private ScheduleBlockRepository blockRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private UserRepository userRepository;
  @Mock private HospitalRepository hospitalRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private SchedulingService schedulingService;

  @Test
  void cancellationPublishesEventOnlyForFirstTransitionToCancelled() {
    UUID hospitalId = UUID.randomUUID();
    UUID appointmentId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    User doctor = User.builder().id(doctorId).fullName("Médico").build();
    Patient patient = Patient.builder().id(UUID.randomUUID()).fullName("Ana Silva").build();
    Appointment appointment =
        Appointment.builder()
            .id(appointmentId)
            .patient(patient)
            .doctor(doctor)
            .appointmentDate(LocalDate.of(2026, 5, 21))
            .startTime(LocalTime.of(10, 30))
            .endTime(LocalTime.of(11, 0))
            .appointmentType(AppointmentType.OUTPATIENT)
            .status(AppointmentStatus.SCHEDULED)
            .reason("Consulta")
            .build();
    appointment.setHospitalId(hospitalId);
    when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
    when(appointmentRepository.save(appointment)).thenReturn(appointment);

    schedulingService.cancel(appointmentId, "Pedido do paciente");
    schedulingService.cancel(appointmentId, "Pedido repetido");

    verify(eventPublisher)
        .publishEvent(
            new AppointmentCancelledEvent(
                hospitalId,
                appointmentId,
                doctorId,
                "Ana Silva",
                LocalDate.of(2026, 5, 21),
                LocalTime.of(10, 30)));
  }

  @Test
  void completedAppointmentsCannotBeCompletedAgain() {
    UUID appointmentId = UUID.randomUUID();
    Appointment appointment =
        Appointment.builder().id(appointmentId).status(AppointmentStatus.COMPLETED).build();
    when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> schedulingService.complete(appointmentId));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    verify(appointmentRepository, never()).save(any(Appointment.class));
  }

  @Test
  void completedAppointmentsCannotBeConfirmed() {
    UUID appointmentId = UUID.randomUUID();
    Appointment appointment =
        Appointment.builder().id(appointmentId).status(AppointmentStatus.COMPLETED).build();
    when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> schedulingService.confirm(appointmentId));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    verify(appointmentRepository, never()).save(any(Appointment.class));
  }

  @Test
  void calendarEventsExposeDoctorIdForFiltering() {
    UUID hospitalId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2026, 5, 21);
    Appointment appointment =
        Appointment.builder()
            .id(UUID.randomUUID())
            .patient(Patient.builder().id(UUID.randomUUID()).fullName("Ana Silva").build())
            .doctor(User.builder().id(doctorId).fullName("Médico").build())
            .appointmentDate(date)
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(10, 30))
            .status(AppointmentStatus.SCHEDULED)
            .build();
    TenantContext.setCurrentHospital(hospitalId);
    when(appointmentRepository.findByHospitalAndDateRange(hospitalId, date, date))
        .thenReturn(List.of(appointment));

    List<CalendarEventResponse> events = schedulingService.getCalendarEvents(date, date);

    assertThat(events)
        .singleElement()
        .extracting(CalendarEventResponse::getDoctorId)
        .isEqualTo(doctorId);
  }

  @Test
  void availabilityChecksPartialBlockOverlapWithTheWholeSlotInterval() {
    UUID doctorId = UUID.randomUUID();
    User doctor = User.builder().id(doctorId).fullName("Médico").build();
    LocalDate date = LocalDate.now().plusDays(1);
    DoctorSchedule schedule =
        DoctorSchedule.builder()
            .doctor(doctor)
            .dayOfWeek(date.getDayOfWeek().getValue() - 1)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .slotDurationMinutes(30)
            .maxPatientsPerSlot(1)
            .build();
    ScheduleBlock block =
        ScheduleBlock.builder()
            .doctor(doctor)
            .blockDate(date)
            .startTime(LocalTime.of(9, 15))
            .endTime(LocalTime.of(9, 30))
            .build();
    when(scheduleRepository.findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(doctorId))
        .thenReturn(List.of(schedule));
    when(blockRepository.findByDoctorAndDate(doctorId, date)).thenReturn(List.of(block));
    when(appointmentRepository.findByDoctorIdAndAppointmentDateOrderByStartTime(doctorId, date))
        .thenReturn(List.of());

    var response = schedulingService.getAvailability(doctorId, date);

    assertThat(response.getSlots())
        .extracting(slot -> slot.isAvailable())
        .containsExactly(false, true);
    assertThat(response.getAvailableSlots()).isEqualTo(1);
  }

  @Test
  void availabilityRespectsCapacityAndIgnoresCancelledAppointments() {
    UUID doctorId = UUID.randomUUID();
    User doctor = User.builder().id(doctorId).fullName("Médico").build();
    LocalDate date = LocalDate.now().plusDays(1);
    DoctorSchedule schedule =
        DoctorSchedule.builder()
            .doctor(doctor)
            .dayOfWeek(date.getDayOfWeek().getValue() - 1)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(9, 30))
            .slotDurationMinutes(30)
            .maxPatientsPerSlot(2)
            .build();
    Appointment booked =
        Appointment.builder()
            .startTime(LocalTime.of(9, 0))
            .status(AppointmentStatus.CONFIRMED)
            .build();
    Appointment cancelled =
        Appointment.builder()
            .startTime(LocalTime.of(9, 0))
            .status(AppointmentStatus.CANCELLED)
            .build();
    when(scheduleRepository.findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(doctorId))
        .thenReturn(List.of(schedule));
    when(blockRepository.findByDoctorAndDate(doctorId, date)).thenReturn(List.of());
    when(appointmentRepository.findByDoctorIdAndAppointmentDateOrderByStartTime(doctorId, date))
        .thenReturn(List.of(booked, cancelled));

    var response = schedulingService.getAvailability(doctorId, date);

    assertThat(response.getSlots()).hasSize(1);
    assertThat(response.getSlots().get(0).getBookedCount()).isEqualTo(1);
    assertThat(response.getSlots().get(0).isAvailable()).isTrue();
  }

  @Test
  void createScheduleRejectsInvalidTimeRangeBeforeQueryingRepositories() {
    CreateScheduleRequest request = new CreateScheduleRequest();
    request.setDoctorId(UUID.randomUUID());
    request.setDayOfWeek(1);
    request.setStartTime(LocalTime.of(11, 0));
    request.setEndTime(LocalTime.of(10, 0));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> schedulingService.createSchedule(request));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(scheduleRepository, never()).existsOverlappingSchedule(any(), anyInt(), any(), any());
    verify(scheduleRepository, never()).save(any(DoctorSchedule.class));
  }

  @Test
  void createScheduleRejectsSlotLongerThanTheConfiguredInterval() {
    CreateScheduleRequest request = new CreateScheduleRequest();
    request.setDoctorId(UUID.randomUUID());
    request.setDayOfWeek(1);
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(10, 15));
    request.setSlotDurationMinutes(30);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> schedulingService.createSchedule(request));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(userRepository, never()).findById(any());
    verify(scheduleRepository, never()).save(any(DoctorSchedule.class));
  }

  @Test
  void createScheduleRejectsAnOverlapForTheSameDoctorAndWeekday() {
    UUID hospitalId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    TenantContext.setCurrentHospital(hospitalId);
    User doctor = User.builder().id(doctorId).fullName("Médico").build();
    CreateScheduleRequest request = new CreateScheduleRequest();
    request.setDoctorId(doctorId);
    request.setDayOfWeek(1);
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 0));
    when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
    when(scheduleRepository.existsOverlappingSchedule(
            doctorId, 1, LocalTime.of(10, 0), LocalTime.of(11, 0)))
        .thenReturn(true);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> schedulingService.createSchedule(request));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    verify(scheduleRepository, never()).save(any(DoctorSchedule.class));
  }

  @org.junit.jupiter.api.AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }
}
