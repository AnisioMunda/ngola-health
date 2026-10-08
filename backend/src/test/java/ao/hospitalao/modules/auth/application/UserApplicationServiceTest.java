package ao.hospitalao.modules.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserApplicationService service;

  @Test
  void onlyExposesMappedTeamsDoctorsForTheRequestedHospital() {
    UUID hospitalId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    UUID teamsUserId = UUID.randomUUID();
    User doctor = User.builder().id(doctorId).fullName("Dr. Test").teamsUserId(teamsUserId).build();
    when(userRepository.findActiveTeamsDoctorsByHospitalId(hospitalId)).thenReturn(List.of(doctor));

    assertThat(service.findTeamsDoctors(hospitalId))
        .containsExactly(
            new UserApplicationService.TeamsDoctor(doctorId, "Dr. Test", teamsUserId.toString()));
    verify(userRepository).findActiveTeamsDoctorsByHospitalId(hospitalId);
  }

  @Test
  void requiresAnActiveMappedDoctorInTheSameHospital() {
    UUID hospitalId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    when(userRepository.findActiveTeamsDoctorByIdAndHospitalId(doctorId, hospitalId))
        .thenReturn(Optional.empty());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.findTeamsDoctor(doctorId, hospitalId))
        .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
        .hasMessageContaining("neste hospital");
  }
}
