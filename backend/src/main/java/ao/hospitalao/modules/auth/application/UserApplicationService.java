package ao.hospitalao.modules.auth.application;

import ao.hospitalao.modules.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserApplicationService {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<TeamsDoctor> findTeamsDoctors(UUID hospitalId) {
    return userRepository.findActiveTeamsDoctorsByHospitalId(hospitalId).stream()
        .map(
            user ->
                new TeamsDoctor(user.getId(), user.getFullName(), user.getTeamsUserId().toString()))
        .toList();
  }

  @Transactional(readOnly = true)
  public TeamsDoctor findTeamsDoctor(UUID userId, UUID hospitalId) {
    return userRepository
        .findActiveTeamsDoctorByIdAndHospitalId(userId, hospitalId)
        .map(
            user ->
                new TeamsDoctor(user.getId(), user.getFullName(), user.getTeamsUserId().toString()))
        .orElseThrow(
            () -> new EntityNotFoundException("Médico Teams não encontrado neste hospital."));
  }

  public record TeamsDoctor(UUID id, String fullName, String organizerId) {}
}
