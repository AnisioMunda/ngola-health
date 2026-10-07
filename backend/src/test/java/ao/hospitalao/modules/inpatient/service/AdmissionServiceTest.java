package ao.hospitalao.modules.inpatient.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.inpatient.dto.InpatientDtos.CreateAdmissionRequest;
import ao.hospitalao.modules.inpatient.entity.Bed;
import ao.hospitalao.modules.inpatient.entity.Bed.BedStatus;
import ao.hospitalao.modules.inpatient.repository.AdmissionRepository;
import ao.hospitalao.modules.inpatient.repository.BedRepository;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.security.tenant.TenantContext;
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
class AdmissionServiceTest {

  @Mock private AdmissionRepository admissionRepository;
  @Mock private BedRepository bedRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private UserRepository userRepository;
  @Mock private HospitalRepository hospitalRepository;
  @Mock private EpisodeRepository episodeRepository;
  @Mock private InpatientUserProvider userProvider;
  @Mock private AdmissionResponseMapper responseMapper;

  @InjectMocks private AdmissionService admissionService;

  @Test
  void locksBedAndRejectsAdmissionWhenItIsAlreadyOccupied() {
    UUID hospitalId = UUID.randomUUID();
    UUID bedId = UUID.randomUUID();
    Bed bed = org.mockito.Mockito.mock(Bed.class);
    when(bed.isActive()).thenReturn(true);
    when(bed.getStatus()).thenReturn(BedStatus.OCCUPIED);
    when(bedRepository.findByIdForUpdate(bedId)).thenReturn(Optional.of(bed));

    CreateAdmissionRequest request = new CreateAdmissionRequest();
    request.setBedId(bedId);

    try (var tenantContext = mockStatic(TenantContext.class)) {
      tenantContext.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

      assertThatThrownBy(() -> admissionService.admit(request))
          .isInstanceOfSatisfying(
              ResponseStatusException.class,
              exception ->
                  org.assertj.core.api.Assertions.assertThat(exception.getStatusCode())
                      .isEqualTo(HttpStatus.CONFLICT));
    }

    verify(bedRepository).findByIdForUpdate(bedId);
    verify(admissionRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }
}
