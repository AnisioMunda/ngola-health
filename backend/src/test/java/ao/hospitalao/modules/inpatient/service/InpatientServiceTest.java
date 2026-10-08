package ao.hospitalao.modules.inpatient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.CreateBedRequest;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.CreateWardRequest;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.UpdateBedStatusRequest;
import ao.hospitalao.modules.inpatient.entity.Admission;
import ao.hospitalao.modules.inpatient.entity.Admission.AdmissionStatus;
import ao.hospitalao.modules.inpatient.entity.Bed;
import ao.hospitalao.modules.inpatient.entity.Bed.BedStatus;
import ao.hospitalao.modules.inpatient.entity.Ward;
import ao.hospitalao.modules.inpatient.repository.AdmissionRepository;
import ao.hospitalao.modules.inpatient.repository.BedRepository;
import ao.hospitalao.modules.inpatient.repository.WardRepository;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.OffsetDateTime;
import java.util.List;
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
class InpatientServiceTest {

  @Mock private WardRepository wardRepository;
  @Mock private BedRepository bedRepository;
  @Mock private AdmissionRepository admissionRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private EpisodeRepository episodeRepository;
  @Mock private UserRepository userRepository;
  @Mock private HospitalRepository hospitalRepository;

  @InjectMocks private InpatientService inpatientService;

  @Test
  void createsWardWithNormalizedCodeAndDefaultType() {
    UUID hospitalId = UUID.randomUUID();
    when(wardRepository.existsByHospitalIdAndCode(hospitalId, "MED-1")).thenReturn(false);
    when(hospitalRepository.getReferenceById(hospitalId)).thenReturn(mock(Hospital.class));
    when(wardRepository.save(any(Ward.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CreateWardRequest request = new CreateWardRequest();
    request.setName("  Medicina  ");
    request.setCode(" med-1 ");

    try (var tenantContext = mockStatic(TenantContext.class)) {
      tenantContext.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

      var response = inpatientService.createWard(request);

      assertThat(response.getName()).isEqualTo("Medicina");
      assertThat(response.getCode()).isEqualTo("MED-1");
      assertThat(response.getType()).isEqualTo(Ward.WardType.GENERAL);
      assertThat(response.getBeds()).isEmpty();
    }
  }

  @Test
  void rejectsDuplicateWardCodeWithConflict() {
    UUID hospitalId = UUID.randomUUID();
    when(wardRepository.existsByHospitalIdAndCode(hospitalId, "MED-1")).thenReturn(true);

    CreateWardRequest request = new CreateWardRequest();
    request.setName("Medicina");
    request.setCode("med-1");

    try (var tenantContext = mockStatic(TenantContext.class)) {
      tenantContext.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

      assertThatThrownBy(() -> inpatientService.createWard(request))
          .isInstanceOfSatisfying(
              ResponseStatusException.class,
              exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
  }

  @Test
  void createsBedAndIncrementsWardBedCount() {
    UUID hospitalId = UUID.randomUUID();
    UUID wardId = UUID.randomUUID();
    Ward ward = Ward.builder().id(wardId).name("Medicina").build();
    when(wardRepository.findById(wardId)).thenReturn(Optional.of(ward));
    when(hospitalRepository.getReferenceById(hospitalId)).thenReturn(mock(Hospital.class));
    when(bedRepository.save(any(Bed.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CreateBedRequest request = new CreateBedRequest();
    request.setWardId(wardId);
    request.setBedNumber("  01  ");

    try (var tenantContext = mockStatic(TenantContext.class)) {
      tenantContext.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

      var response = inpatientService.createBed(request);

      assertThat(response.getBedNumber()).isEqualTo("01");
      assertThat(response.getStatus()).isEqualTo(BedStatus.AVAILABLE);
      assertThat(ward.getTotalBeds()).isEqualTo(1);
      verify(wardRepository).save(ward);
    }
  }

  @Test
  void bedListIncludesActivePatientAdmissionWithoutPerBedQueries() {
    UUID wardId = UUID.randomUUID();
    UUID bedId = UUID.randomUUID();
    UUID admissionId = UUID.randomUUID();
    OffsetDateTime admissionDate = OffsetDateTime.now();
    Ward ward = Ward.builder().id(wardId).name("Medicina").build();
    Bed bed = Bed.builder().id(bedId).ward(ward).bedNumber("01").status(BedStatus.OCCUPIED).build();
    Patient patient = mock(Patient.class);
    when(patient.getFullName()).thenReturn("Ana Silva");
    Admission admission = mock(Admission.class);
    when(admission.getBed()).thenReturn(bed);
    when(admission.getPatient()).thenReturn(patient);
    when(admission.getId()).thenReturn(admissionId);
    when(admission.getAdmissionDate()).thenReturn(admissionDate);
    when(bedRepository.findByWardIdAndActiveTrueOrderByBedNumber(wardId)).thenReturn(List.of(bed));
    when(admissionRepository.findByWardIdAndStatus(wardId, AdmissionStatus.ACTIVE))
        .thenReturn(List.of(admission));

    var beds = inpatientService.findBedsByWard(wardId);

    assertThat(beds)
        .singleElement()
        .satisfies(
            response -> {
              assertThat(response.getPatientName()).isEqualTo("Ana Silva");
              assertThat(response.getAdmissionId()).isEqualTo(admissionId);
              assertThat(response.getAdmissionDate()).isEqualTo(admissionDate);
            });
    verify(admissionRepository).findByWardIdAndStatus(wardId, AdmissionStatus.ACTIVE);
  }

  @Test
  void preventsMarkingBedOccupiedOutsideAnAdmission() {
    UpdateBedStatusRequest request = new UpdateBedStatusRequest();
    request.setStatus(BedStatus.OCCUPIED);

    assertThatThrownBy(() -> inpatientService.updateBedStatus(UUID.randomUUID(), request))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    verify(bedRepository, never()).save(any(Bed.class));
  }

  @Test
  void preventsChangingBedStatusWhileAnActiveAdmissionExists() {
    UUID bedId = UUID.randomUUID();
    Bed bed =
        Bed.builder()
            .id(bedId)
            .ward(Ward.builder().id(UUID.randomUUID()).name("Medicina").build())
            .bedNumber("01")
            .status(BedStatus.OCCUPIED)
            .build();
    UpdateBedStatusRequest request = new UpdateBedStatusRequest();
    request.setStatus(BedStatus.MAINTENANCE);
    when(bedRepository.findByIdForUpdate(bedId)).thenReturn(Optional.of(bed));
    when(admissionRepository.isBedOccupied(bedId)).thenReturn(true);

    assertThatThrownBy(() -> inpatientService.updateBedStatus(bedId, request))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    verify(bedRepository, never()).save(any(Bed.class));
  }

  @Test
  void updatesBedStatusWhenNoActiveAdmissionExists() {
    UUID bedId = UUID.randomUUID();
    Bed bed =
        Bed.builder()
            .id(bedId)
            .ward(Ward.builder().id(UUID.randomUUID()).name("Medicina").build())
            .bedNumber("01")
            .status(BedStatus.AVAILABLE)
            .build();
    UpdateBedStatusRequest request = new UpdateBedStatusRequest();
    request.setStatus(BedStatus.MAINTENANCE);
    request.setNotes("Aguarda reparação");
    when(bedRepository.findByIdForUpdate(bedId)).thenReturn(Optional.of(bed));
    when(admissionRepository.isBedOccupied(bedId)).thenReturn(false);
    when(bedRepository.save(any(Bed.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response = inpatientService.updateBedStatus(bedId, request);

    assertThat(response.getStatus()).isEqualTo(BedStatus.MAINTENANCE);
    assertThat(response.getNotes()).isEqualTo("Aguarda reparação");
    verify(bedRepository).save(bed);
  }
}
