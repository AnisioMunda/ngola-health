package ao.hospitalao.modules.hospitals.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.hospitals.dto.CreateHospitalRequest;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.entity.Hospital.HospitalType;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HospitalServiceTest {

  @Mock private HospitalRepository hospitalRepository;

  @InjectMocks private HospitalService hospitalService;

  @Test
  void updateChangesHospitalDetailsAndNormalizesCode() {
    UUID id = UUID.randomUUID();
    Hospital existing = Hospital.builder().id(id).code("HCL-001").build();
    when(hospitalRepository.findById(id)).thenReturn(Optional.of(existing));
    when(hospitalRepository.existsByCodeAndIdNot("HCL-002", id)).thenReturn(false);
    when(hospitalRepository.save(any(Hospital.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = hospitalService.update(id, request("hcl-002"));

    assertThat(response.getCode()).isEqualTo("HCL-002");
    assertThat(response.getName()).isEqualTo("Hospital de Teste");
  }

  @Test
  void updateRejectsHospitalCodeUsedByAnotherHospital() {
    UUID id = UUID.randomUUID();
    when(hospitalRepository.findById(id))
        .thenReturn(Optional.of(Hospital.builder().id(id).code("HCL-001").build()));
    when(hospitalRepository.existsByCodeAndIdNot("HCL-002", id)).thenReturn(true);

    assertThatThrownBy(() -> hospitalService.update(id, request("hcl-002")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("code already in use");
  }

  @Test
  void updateRejectsUnknownHospital() {
    UUID id = UUID.randomUUID();
    when(hospitalRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> hospitalService.update(id, request("HCL-002")))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  private CreateHospitalRequest request(String code) {
    CreateHospitalRequest request = new CreateHospitalRequest();
    request.setName("Hospital de Teste");
    request.setCode(code);
    request.setType(HospitalType.HOSPITAL);
    request.setProvince("Luanda");
    return request;
  }
}
