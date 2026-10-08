package ao.hospitalao.application.inpatient;

import ao.hospitalao.modules.inpatient.dto.InpatientDtos.AdmissionResponse;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.DischargeRequest;
import ao.hospitalao.modules.inpatient.entity.Admission;
import ao.hospitalao.modules.inpatient.entity.Admission.AdmissionStatus;
import ao.hospitalao.modules.inpatient.entity.Bed;
import ao.hospitalao.modules.inpatient.entity.Bed.BedStatus;
import ao.hospitalao.modules.inpatient.repository.AdmissionRepository;
import ao.hospitalao.modules.inpatient.repository.BedRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DischargeService {

  private final AdmissionRepository admissionRepository;
  private final BedRepository bedRepository;
  private final InpatientUserProvider userProvider;
  private final AdmissionResponseMapper responseMapper;

  @Transactional
  public AdmissionResponse discharge(UUID id, DischargeRequest request) {
    Admission admission =
        admissionRepository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new EntityNotFoundException("Internamento não encontrado: " + id));
    if (admission.getStatus() != AdmissionStatus.ACTIVE) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Apenas internamentos activos podem receber alta.");
    }

    Bed bed =
        bedRepository
            .findByIdForUpdate(admission.getBed().getId())
            .orElseThrow(() -> new EntityNotFoundException("Cama não encontrada"));
    if (bed.getStatus() != BedStatus.OCCUPIED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A cama associada ao internamento não está ocupada.");
    }

    admission.setStatus(
        request.getDischargeCondition() == Admission.DischargeCondition.DECEASED
            ? AdmissionStatus.DECEASED
            : AdmissionStatus.DISCHARGED);
    admission.setDischargeDate(OffsetDateTime.now());
    admission.setDischargeNotes(request.getDischargeNotes());
    admission.setDischargeCondition(request.getDischargeCondition());
    admission.setDischargedBy(userProvider.getCurrentUser());
    bed.setStatus(BedStatus.AVAILABLE);
    bedRepository.save(bed);

    log.info("Patient discharged from inpatient care");
    return responseMapper.toResponse(admissionRepository.save(admission));
  }
}
