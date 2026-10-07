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
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.DischargeRequest;
import ao.hospitalao.modules.inpatient.entity.Admission;
import ao.hospitalao.modules.inpatient.entity.Admission.AdmissionStatus;
import ao.hospitalao.modules.inpatient.entity.Admission.DischargeCondition;
import ao.hospitalao.modules.inpatient.entity.Bed;
import ao.hospitalao.modules.inpatient.entity.Bed.BedStatus;
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
class DischargeServiceTest {

  @Mock private AdmissionRepository admissionRepository;
  @Mock private BedRepository bedRepository;
  @Mock private InpatientUserProvider userProvider;
  @Mock private AdmissionResponseMapper responseMapper;

  @InjectMocks private DischargeService dischargeService;

  @Test
  void dischargesPatientAndReleasesBed() {
    UUID admissionId = UUID.randomUUID();
    UUID bedId = UUID.randomUUID();
    Bed bed =
        Bed.builder()
            .id(bedId)
            .bedNumber("01")
            .status(BedStatus.OCCUPIED)
            .ward(ao.hospitalao.modules.inpatient.entity.Ward.builder().name("Medicina").build())
            .build();
    Patient patient = mock(Patient.class);
    when(patient.getFullName()).thenReturn("Ana Silva");
    Admission admission =
        Admission.builder()
            .id(admissionId)
            .status(AdmissionStatus.ACTIVE)
            .bed(bed)
            .patient(patient)
            .build();
    User user = mock(User.class);
    AdmissionResponse response = mock(AdmissionResponse.class);
    DischargeRequest request = new DischargeRequest();
    request.setDischargeCondition(DischargeCondition.STABLE);
    request.setDischargeNotes("Alta clínica");
    when(admissionRepository.findByIdForUpdate(admissionId)).thenReturn(Optional.of(admission));
    when(bedRepository.findByIdForUpdate(bedId)).thenReturn(Optional.of(bed));
    when(userProvider.getCurrentUser()).thenReturn(user);
    when(admissionRepository.save(admission)).thenReturn(admission);
    when(responseMapper.toResponse(admission)).thenReturn(response);

    assertThat(dischargeService.discharge(admissionId, request)).isSameAs(response);

    assertThat(admission.getStatus()).isEqualTo(AdmissionStatus.DISCHARGED);
    assertThat(admission.getDischargeDate()).isNotNull();
    assertThat(admission.getDischargeNotes()).isEqualTo("Alta clínica");
    assertThat(admission.getDischargedBy()).isSameAs(user);
    assertThat(bed.getStatus()).isEqualTo(BedStatus.AVAILABLE);
    verify(bedRepository).save(bed);
  }

  @Test
  void rejectsDischargingAnAdmissionThatIsNotActive() {
    UUID admissionId = UUID.randomUUID();
    Admission admission = Admission.builder().status(AdmissionStatus.DISCHARGED).build();
    when(admissionRepository.findByIdForUpdate(admissionId)).thenReturn(Optional.of(admission));

    assertThatThrownBy(() -> dischargeService.discharge(admissionId, new DischargeRequest()))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

    verify(bedRepository, never()).findByIdForUpdate(any(UUID.class));
    verify(admissionRepository, never()).save(any(Admission.class));
  }
}
