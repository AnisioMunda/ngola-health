package ao.hospitalao.modules.scheduling.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.notifications.event.AppointmentCancelledEvent;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.scheduling.entity.Appointment;
import ao.hospitalao.modules.scheduling.entity.Appointment.AppointmentStatus;
import ao.hospitalao.modules.scheduling.entity.Appointment.AppointmentType;
import ao.hospitalao.modules.scheduling.repository.AppointmentRepository;
import ao.hospitalao.modules.scheduling.repository.DoctorScheduleRepository;
import ao.hospitalao.modules.scheduling.repository.ScheduleBlockRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
}
