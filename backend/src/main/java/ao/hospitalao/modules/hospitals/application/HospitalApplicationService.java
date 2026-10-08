package ao.hospitalao.modules.hospitals.application;

import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HospitalApplicationService {

  private final HospitalRepository hospitalRepository;

  public Hospital getReferenceById(UUID hospitalId) {
    return hospitalRepository.getReferenceById(hospitalId);
  }
}
