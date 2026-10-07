package ao.hospitalao.modules.hospitals.service;

import ao.hospitalao.modules.hospitals.dto.CreateHospitalRequest;
import ao.hospitalao.modules.hospitals.dto.HospitalResponse;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalService {

  private final HospitalRepository hospitalRepository;

  @Transactional(readOnly = true)
  public List<HospitalResponse> findAll(boolean includeInactive) {
    List<Hospital> hospitals =
        includeInactive ? hospitalRepository.findAll() : hospitalRepository.findByActiveTrue();
    return hospitals.stream().map(this::toResponse).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public HospitalResponse findById(UUID id) {
    return hospitalRepository
        .findById(id)
        .map(this::toResponse)
        .orElseThrow(() -> new EntityNotFoundException("Hospital not found: " + id));
  }

  @Transactional
  public HospitalResponse create(CreateHospitalRequest request) {
    String code = request.getCode().toUpperCase(Locale.ROOT);
    if (hospitalRepository.existsByCode(code)) {
      throw new IllegalArgumentException("Hospital code already in use: " + request.getCode());
    }

    Hospital hospital =
        Hospital.builder()
            .name(request.getName())
            .code(code)
            .type(request.getType())
            .province(request.getProvince())
            .municipality(request.getMunicipality())
            .address(request.getAddress())
            .phone(request.getPhone())
            .email(request.getEmail())
            .taxId(request.getTaxId())
            .build();

    Hospital saved = hospitalRepository.save(hospital);
    log.info("Hospital created: {} ({})", saved.getName(), saved.getId());
    return toResponse(saved);
  }

  @Transactional
  public HospitalResponse update(UUID id, CreateHospitalRequest request) {
    Hospital hospital =
        hospitalRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Hospital not found: " + id));

    String code = request.getCode().toUpperCase(Locale.ROOT);
    if (hospitalRepository.existsByCodeAndIdNot(code, id)) {
      throw new IllegalArgumentException("Hospital code already in use: " + request.getCode());
    }

    hospital.setName(request.getName());
    hospital.setCode(code);
    hospital.setType(request.getType());
    hospital.setProvince(request.getProvince());
    hospital.setMunicipality(request.getMunicipality());
    hospital.setAddress(request.getAddress());
    hospital.setPhone(request.getPhone());
    hospital.setEmail(request.getEmail());
    hospital.setTaxId(request.getTaxId());

    Hospital saved = hospitalRepository.save(hospital);
    log.info("Hospital updated: {} ({})", saved.getName(), saved.getId());
    return toResponse(saved);
  }

  @Transactional
  public HospitalResponse setActive(UUID id, boolean active) {
    Hospital hospital =
        hospitalRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Hospital not found: " + id));
    hospital.setActive(active);
    return toResponse(hospitalRepository.save(hospital));
  }

  private HospitalResponse toResponse(Hospital h) {
    return HospitalResponse.builder()
        .id(h.getId())
        .name(h.getName())
        .code(h.getCode())
        .type(h.getType())
        .province(h.getProvince())
        .municipality(h.getMunicipality())
        .address(h.getAddress())
        .phone(h.getPhone())
        .email(h.getEmail())
        .taxId(h.getTaxId())
        .active(h.isActive())
        .build();
  }
}
