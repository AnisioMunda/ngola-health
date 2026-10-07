package ao.hospitalao.modules.inpatient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.AdmissionResponse;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.TransferRequest;
import ao.hospitalao.modules.inpatient.entity.Admission;
import ao.hospitalao.modules.inpatient.entity.Admission.AdmissionStatus;
import ao.hospitalao.modules.inpatient.entity.Bed;
import ao.hospitalao.modules.inpatient.entity.Bed.BedStatus;
import ao.hospitalao.modules.inpatient.entity.Ward;
import ao.hospitalao.modules.inpatient.repository.AdmissionRepository;
import ao.hospitalao.modules.inpatient.repository.BedRepository;
import ao.hospitalao.modules.patients.entity.Patient;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BedTransferServiceTest {

  @Mock private AdmissionRepository admissionRepository;
  @Mock private BedRepository bedRepository;
  @Mock private InpatientUserProvider userProvider;
  @Mock private AdmissionResponseMapper responseMapper;

  @InjectMocks private BedTransferService bedTransferService;

  @Test
  void transfersPatientAndLocksBothBedsInIdOrder() {
    UUID admissionId = UUID.randomUUID();
    UUID oldBedId = UUID.randomUUID();
    UUID newBedId = UUID.randomUUID();
    Ward oldWard = Ward.builder().id(UUID.randomUUID()).name("Medicina").build();
    Ward newWard = Ward.builder().id(UUID.randomUUID()).name("Cirurgia").build();
    Bed oldBed =
        Bed.builder().id(oldBedId).bedNumber("01").status(BedStatus.OCCUPIED).ward(oldWard).build();
    Bed newBed =
        Bed.builder()
            .id(newBedId)
            .bedNumber("02")
            .status(BedStatus.AVAILABLE)
            .ward(newWard)
            .build();
    Patient patient = mock(Patient.class);
    when(patient.getFullName()).thenReturn("Ana Silva");
    Admission admission =
        Admission.builder()
            .id(admissionId)
            .status(AdmissionStatus.ACTIVE)
            .bed(oldBed)
            .ward(oldWard)
            .patient(patient)
            .build();
    AdmissionResponse response = mock(AdmissionResponse.class);
    TransferRequest request = new TransferRequest();
    request.setToBedId(newBedId);
    request.setReason("Transferência clínica");
    when(admissionRepository.findByIdForUpdate(admissionId)).thenReturn(Optional.of(admission));
    when(bedRepository.findByIdForUpdate(oldBedId)).thenReturn(Optional.of(oldBed));
    when(bedRepository.findByIdForUpdate(newBedId)).thenReturn(Optional.of(newBed));
    when(userProvider.getCurrentUser()).thenReturn(mock(User.class));
    when(admissionRepository.save(admission)).thenReturn(admission);
    when(responseMapper.toResponse(admission)).thenReturn(response);

    assertThat(bedTransferService.transfer(admissionId, request)).isSameAs(response);

    assertThat(admission.getBed()).isSameAs(newBed);
    assertThat(admission.getWard()).isSameAs(newWard);
    assertThat(admission.getTransfers())
        .singleElement()
        .satisfies(
            transfer -> {
              assertThat(transfer.getFromBed()).isSameAs(oldBed);
              assertThat(transfer.getToBed()).isSameAs(newBed);
              assertThat(transfer.getReason()).isEqualTo("Transferência clínica");
            });
    assertThat(oldBed.getStatus()).isEqualTo(BedStatus.AVAILABLE);
    assertThat(newBed.getStatus()).isEqualTo(BedStatus.OCCUPIED);
    if (oldBedId.compareTo(newBedId) < 0) {
      org.mockito.InOrder order = org.mockito.Mockito.inOrder(bedRepository);
      order.verify(bedRepository).findByIdForUpdate(oldBedId);
      order.verify(bedRepository).findByIdForUpdate(newBedId);
    } else {
      org.mockito.InOrder order = org.mockito.Mockito.inOrder(bedRepository);
      order.verify(bedRepository).findByIdForUpdate(newBedId);
      order.verify(bedRepository).findByIdForUpdate(oldBedId);
    }
  }

  @Test
  void rejectsTransferToAnOccupiedBed() {
    UUID admissionId = UUID.randomUUID();
    UUID oldBedId = UUID.randomUUID();
    UUID newBedId = UUID.randomUUID();
    Bed oldBed =
        Bed.builder()
            .id(oldBedId)
            .status(BedStatus.OCCUPIED)
            .ward(Ward.builder().name("Medicina").build())
            .build();
    Bed newBed =
        Bed.builder()
            .id(newBedId)
            .status(BedStatus.OCCUPIED)
            .ward(Ward.builder().name("Cirurgia").build())
            .build();
    Admission admission =
        Admission.builder()
            .id(admissionId)
            .status(AdmissionStatus.ACTIVE)
            .bed(oldBed)
            .ward(oldBed.getWard())
            .build();
    TransferRequest request = new TransferRequest();
    request.setToBedId(newBedId);
    when(admissionRepository.findByIdForUpdate(admissionId)).thenReturn(Optional.of(admission));
    when(bedRepository.findByIdForUpdate(oldBedId)).thenReturn(Optional.of(oldBed));
    when(bedRepository.findByIdForUpdate(newBedId)).thenReturn(Optional.of(newBed));

    assertThatThrownBy(() -> bedTransferService.transfer(admissionId, request))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

    verify(admissionRepository, never()).save(any(Admission.class));
  }
}
