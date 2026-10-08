package ao.hospitalao.application.inpatient;

import ao.hospitalao.modules.inpatient.dto.InpatientDtos.AdmissionResponse;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.TransferRequest;
import ao.hospitalao.modules.inpatient.entity.Admission;
import ao.hospitalao.modules.inpatient.entity.Admission.AdmissionStatus;
import ao.hospitalao.modules.inpatient.entity.Bed;
import ao.hospitalao.modules.inpatient.entity.Bed.BedStatus;
import ao.hospitalao.modules.inpatient.entity.BedTransfer;
import ao.hospitalao.modules.inpatient.repository.AdmissionRepository;
import ao.hospitalao.modules.inpatient.repository.BedRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BedTransferService {

  private final AdmissionRepository admissionRepository;
  private final BedRepository bedRepository;
  private final InpatientUserProvider userProvider;
  private final AdmissionResponseMapper responseMapper;

  @Transactional
  public AdmissionResponse transfer(UUID id, TransferRequest request) {
    Admission admission =
        admissionRepository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new EntityNotFoundException("Internamento não encontrado: " + id));
    if (admission.getStatus() != AdmissionStatus.ACTIVE) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Apenas internamentos activos podem ser transferidos.");
    }

    UUID oldBedId = admission.getBed().getId();
    UUID newBedId = request.getToBedId();
    Map<UUID, Bed> lockedBeds = new HashMap<>();
    Stream.of(oldBedId, newBedId)
        .distinct()
        .sorted()
        .forEach(bedId -> lockedBeds.put(bedId, lockBed(bedId)));
    Bed oldBed = lockedBeds.get(oldBedId);
    Bed newBed = lockedBeds.get(newBedId);

    if (oldBedId.equals(newBedId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O paciente já se encontra nesta cama.");
    }
    if (oldBed.getStatus() != BedStatus.OCCUPIED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A cama actual do internamento não está ocupada.");
    }
    if (!newBed.isActive()
        || (newBed.getStatus() != BedStatus.AVAILABLE
            && newBed.getStatus() != BedStatus.RESERVED)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A cama de destino não está disponível.");
    }

    admission
        .getTransfers()
        .add(
            BedTransfer.builder()
                .admission(admission)
                .fromBed(oldBed)
                .fromWard(admission.getWard())
                .toBed(newBed)
                .toWard(newBed.getWard())
                .reason(request.getReason() != null ? request.getReason().trim() : null)
                .transferredBy(userProvider.getCurrentUser())
                .build());

    oldBed.setStatus(BedStatus.AVAILABLE);
    newBed.setStatus(BedStatus.OCCUPIED);
    admission.setBed(newBed);
    admission.setWard(newBed.getWard());
    bedRepository.save(oldBed);
    bedRepository.save(newBed);

    log.info("Inpatient bed transfer completed");
    return responseMapper.toResponse(admissionRepository.save(admission));
  }

  private Bed lockBed(UUID id) {
    return bedRepository
        .findByIdForUpdate(id)
        .orElseThrow(() -> new EntityNotFoundException("Cama não encontrada: " + id));
  }
}
