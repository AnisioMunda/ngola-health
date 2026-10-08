package ao.hospitalao.modules.scheduling.application;

import ao.hospitalao.modules.scheduling.repository.AppointmentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TelemedicineAppointmentApplicationService {

  private final AppointmentRepository appointmentRepository;

  @Transactional(readOnly = true)
  public void validateForTelemedicine(
      UUID appointmentId, UUID hospitalId, UUID patientId, UUID doctorId) {
    if (!appointmentRepository.existsById(appointmentId)) {
      throw new EntityNotFoundException("Marcação não encontrada.");
    }
    if (!appointmentRepository.existsByIdAndHospitalIdAndPatient_IdAndDoctor_Id(
        appointmentId, hospitalId, patientId, doctorId)) {
      throw new AccessDeniedException(
          "A marcação não pertence ao paciente, médico e hospital seleccionados.");
    }
  }
}
