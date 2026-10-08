package ao.hospitalao.modules.scheduling.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.scheduling.repository.AppointmentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TelemedicineAppointmentApplicationServiceTest {

  @Mock private AppointmentRepository appointmentRepository;

  @InjectMocks private TelemedicineAppointmentApplicationService service;

  @Test
  void acceptsAppointmentsForTheSelectedHospitalPatientAndDoctor() {
    UUID appointmentId = UUID.randomUUID();
    UUID hospitalId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    when(appointmentRepository.existsById(appointmentId)).thenReturn(true);
    when(appointmentRepository.existsByIdAndHospitalIdAndPatient_IdAndDoctor_Id(
            appointmentId, hospitalId, patientId, doctorId))
        .thenReturn(true);

    assertThatCode(
            () -> service.validateForTelemedicine(appointmentId, hospitalId, patientId, doctorId))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsAppointmentsOwnedByAnotherPatientOrDoctor() {
    UUID appointmentId = UUID.randomUUID();
    UUID hospitalId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    when(appointmentRepository.existsById(appointmentId)).thenReturn(true);
    when(appointmentRepository.existsByIdAndHospitalIdAndPatient_IdAndDoctor_Id(
            appointmentId, hospitalId, patientId, doctorId))
        .thenReturn(false);

    assertThatThrownBy(
            () -> service.validateForTelemedicine(appointmentId, hospitalId, patientId, doctorId))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("não pertence");
  }

  @Test
  void rejectsAppointmentsThatDoNotExist() {
    UUID appointmentId = UUID.randomUUID();
    when(appointmentRepository.existsById(appointmentId)).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.validateForTelemedicine(
                    appointmentId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("não encontrada");
  }
}
