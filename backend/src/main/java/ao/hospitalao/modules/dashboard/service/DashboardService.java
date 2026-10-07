package ao.hospitalao.modules.dashboard.service;

import ao.hospitalao.modules.auth.entity.enums.RegisterStatus;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.dashboard.dto.DashboardResponse;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final PatientRepository patientRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public DashboardResponse getDashboard() {

    // Users by role
    Map<String, Long> usersByRole = new LinkedHashMap<>();
    userRepository.countByRole().forEach(r -> usersByRole.put(r.getRoleName(), r.getCount()));

    // Patients by province (top 5)
    List<DashboardResponse.ProvinceCount> patientsByProvince =
        patientRepository.countByProvince(PageRequest.of(0, 5)).stream()
            .map(
                p ->
                    DashboardResponse.ProvinceCount.builder()
                        .province(p.getProvince())
                        .count(p.getCount())
                        .build())
            .toList();

    // Recent patients
    List<DashboardResponse.RecentPatient> recentPatients =
        patientRepository.findTop5ByActiveTrueOrderByCreatedAtDesc().stream()
            .map(
                p ->
                    DashboardResponse.RecentPatient.builder()
                        .id(p.getId().toString())
                        .fullName(p.getFullName())
                        .createdAt(p.getCreatedAt().toString())
                        .build())
            .toList();

    OffsetDateTime oneWeekAgo = OffsetDateTime.now().minusDays(7);

    return DashboardResponse.builder()
        .totalPatients(patientRepository.countByActiveTrue())
        .totalUsers(userRepository.count())
        .activeUsers(userRepository.countByRegisterStatus(RegisterStatus.ACTIVE))
        .malePatients(patientRepository.countByActiveTrueAndGender(Patient.Gender.MALE))
        .femalePatients(patientRepository.countByActiveTrueAndGender(Patient.Gender.FEMALE))
        .newPatientsThisWeek(patientRepository.countByActiveTrueAndCreatedAtAfter(oneWeekAgo))
        .usersByRole(usersByRole)
        .patientsByProvince(patientsByProvince)
        .recentPatients(recentPatients)
        .build();
  }
}
