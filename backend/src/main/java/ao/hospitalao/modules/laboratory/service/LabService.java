package ao.hospitalao.modules.laboratory.service;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.hospitals.entity.TenantScopedEntity;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.laboratory.dto.LabDtos.*;
import ao.hospitalao.modules.laboratory.entity.LabRequest;
import ao.hospitalao.modules.laboratory.entity.LabRequest.RequestStatus;
import ao.hospitalao.modules.laboratory.entity.LabRequestItem;
import ao.hospitalao.modules.laboratory.entity.LabTest;
import ao.hospitalao.modules.laboratory.repository.LabRequestRepository;
import ao.hospitalao.modules.laboratory.repository.LabTestRepository;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabService {

  private final LabRequestRepository requestRepository;
  private final LabTestRepository testRepository;
  private final PatientRepository patientRepository;
  private final EpisodeRepository episodeRepository;
  private final UserRepository userRepository;
  private final HospitalRepository hospitalRepository;

  // ------------------------------------------------
  // Lab Tests (catálogo)
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public List<LabTestResponse> findAllTests() {
    UUID hospitalId = requireHospitalContext();
    return testRepository.findByHospitalIdAndActiveTrue(hospitalId).stream()
        .map(this::toTestResponse)
        .collect(Collectors.toList());
  }

  @SuppressWarnings("null")
  @Transactional
  public LabTestResponse createTest(CreateLabTestRequest req) {
    UUID hospitalId = requireHospitalContext();
    String code = req.getCode().trim().toUpperCase(Locale.ROOT);
    if (testRepository.existsByHospitalIdAndCode(hospitalId, code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "O código do exame já existe.");
    }

    LabTest test =
        LabTest.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .code(code)
            .name(req.getName().trim())
            .category(req.getCategory())
            .sampleType(req.getSampleType())
            .turnaroundHours(req.getTurnaroundHours() != null ? req.getTurnaroundHours() : 24)
            .price(req.getPrice())
            .referenceValues(req.getReferenceValues())
            .build();

    return toTestResponse(testRepository.save(test));
  }

  // ------------------------------------------------
  // Lab Requests
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public Page<LabRequestResponse> findAll(UUID patientId, RequestStatus status, Pageable pageable) {
    UUID hospitalId = requireHospitalContext();
    return requestRepository
        .findWithFilters(hospitalId, patientId, status, pageable)
        .map(this::toRequestResponse);
  }

  @SuppressWarnings("null")
  @Transactional(readOnly = true)
  public LabRequestResponse findById(UUID id) {
    requireHospitalContext();
    return requestRepository
        .findById(id)
        .map(this::toRequestResponse)
        .orElseThrow(() -> new EntityNotFoundException("Lab request not found: " + id));
  }

  @SuppressWarnings("null")
  @Transactional
  public LabRequestResponse create(CreateLabRequestRequest req) {
    UUID hospitalId = requireHospitalContext();
    User currentUser = getCurrentUser();
    requireHospital(currentUser, hospitalId, "Utilizador autenticado não encontrado.");

    var patient =
        patientRepository
            .findById(req.getPatientId())
            .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado."));
    requireHospital(patient, hospitalId, "Paciente não encontrado.");

    var request =
        LabRequest.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .patient(patient)
            .status(RequestStatus.PENDING)
            .priority(req.getPriority() != null ? req.getPriority() : LabRequest.Priority.NORMAL)
            .clinicalNotes(req.getClinicalNotes())
            .createdBy(currentUser)
            .build();

    if (req.getEpisodeId() != null) {
      var episode =
          episodeRepository
              .findById(req.getEpisodeId())
              .orElseThrow(() -> new EntityNotFoundException("Episódio não encontrado."));
      requireHospital(episode, hospitalId, "Episódio não encontrado.");
      if (!episode.getPatient().getId().equals(patient.getId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "O episódio não pertence ao paciente indicado.");
      }
      request.setEpisode(episode);
    }
    if (req.getRequestedById() != null) {
      var requester =
          userRepository
              .findById(req.getRequestedById())
              .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));
      requireHospital(requester, hospitalId, "Profissional não encontrado.");
      request.setRequestedBy(requester);
    } else {
      request.setRequestedBy(currentUser);
    }

    Set<UUID> uniqueTestIds = new HashSet<>();
    for (UUID testId : req.getLabTestIds()) {
      if (!uniqueTestIds.add(testId)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Um exame não pode ser repetido no mesmo pedido.");
      }
      LabTest test =
          testRepository
              .findById(testId)
              .orElseThrow(() -> new EntityNotFoundException("Exame não encontrado."));
      requireHospital(test, hospitalId, "Exame não encontrado.");
      if (!test.isActive()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Não é possível solicitar um exame inactivo.");
      }
      LabRequestItem item = LabRequestItem.builder().request(request).labTest(test).build();
      request.getItems().add(item);
    }

    LabRequest saved = requestRepository.save(request);
    log.info("Lab request created: {} for patient {}", saved.getId(), patient.getFullName());
    return toRequestResponse(saved);
  }

  @Transactional
  public LabRequestResponse collect(UUID id) {
    LabRequest request = getOrThrow(id);
    requireStatus(request, RequestStatus.PENDING, "recolher a amostra");
    request.setStatus(RequestStatus.COLLECTED);
    request.setCollectedAt(OffsetDateTime.now());
    return toRequestResponse(requestRepository.save(request));
  }

  @Transactional
  public LabRequestResponse startAnalysis(UUID id) {
    LabRequest request = getOrThrow(id);
    requireStatus(request, RequestStatus.COLLECTED, "iniciar a análise");
    request.setStatus(RequestStatus.IN_ANALYSIS);
    return toRequestResponse(requestRepository.save(request));
  }

  @Transactional
  public LabRequestResponse submitItemResult(UUID requestId, UUID itemId, SubmitResultRequest req) {
    LabRequest request = getOrThrow(requestId);
    requireStatus(request, RequestStatus.IN_ANALYSIS, "registar resultados");

    LabRequestItem item =
        request.getItems().stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemId));

    if (item.getResultValue() != null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "O resultado deste exame já foi registado.");
    }
    item.setResultValue(req.getResultValue());
    item.setResultUnit(req.getResultUnit());
    item.setReferenceRange(req.getReferenceRange());
    item.setAbnormal(req.isAbnormal());
    item.setResultNotes(req.getResultNotes());
    item.setResultedAt(OffsetDateTime.now());
    item.setResultedBy(getCurrentUser());

    // Se todos os itens têm resultado, completar o pedido
    boolean allResulted = request.getItems().stream().allMatch(i -> i.getResultValue() != null);
    if (allResulted) {
      request.setStatus(RequestStatus.COMPLETED);
      request.setCompletedAt(OffsetDateTime.now());
    }

    return toRequestResponse(requestRepository.save(request));
  }

  @Transactional
  public LabRequestResponse cancel(UUID id) {
    LabRequest request = getOrThrow(id);
    if (request.getStatus() == RequestStatus.COMPLETED
        || request.getStatus() == RequestStatus.CANCELLED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Não é possível cancelar um pedido concluído ou já cancelado.");
    }
    request.setStatus(RequestStatus.CANCELLED);
    return toRequestResponse(requestRepository.save(request));
  }

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  @SuppressWarnings("null")
  private LabRequest getOrThrow(UUID id) {
    requireHospitalContext();
    return requestRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Lab request not found: " + id));
  }

  private User getCurrentUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new EntityNotFoundException("Utilizador autenticado não encontrado.");
    }
    return userRepository
        .findByUsername(authentication.getName())
        .orElseThrow(() -> new EntityNotFoundException("Utilizador autenticado não encontrado."));
  }

  private void requireHospital(TenantScopedEntity entity, UUID hospitalId, String notFoundMessage) {
    if (!hospitalId.equals(entity.getHospitalId())) {
      throw new EntityNotFoundException(notFoundMessage);
    }
  }

  private UUID requireHospitalContext() {
    UUID hospitalId = TenantContext.getCurrentHospital();
    if (hospitalId == null || TenantContext.hasPlatformAccess()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "As operações de laboratório exigem um hospital activo.");
    }
    return hospitalId;
  }

  private void requireStatus(LabRequest request, RequestStatus expected, String action) {
    if (request.getStatus() != expected) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Não é possível " + action + " neste estado do pedido.");
    }
  }

  private LabTestResponse toTestResponse(LabTest t) {
    return LabTestResponse.builder()
        .id(t.getId())
        .code(t.getCode())
        .name(t.getName())
        .category(t.getCategory())
        .sampleType(t.getSampleType())
        .turnaroundHours(t.getTurnaroundHours())
        .price(t.getPrice())
        .referenceValues(t.getReferenceValues())
        .active(t.isActive())
        .build();
  }

  private LabRequestResponse toRequestResponse(LabRequest r) {
    List<LabRequestItemResponse> items =
        r.getItems().stream()
            .map(
                i ->
                    LabRequestItemResponse.builder()
                        .id(i.getId())
                        .labTestId(i.getLabTest().getId())
                        .testCode(i.getLabTest().getCode())
                        .testName(i.getLabTest().getName())
                        .resultValue(i.getResultValue())
                        .resultUnit(i.getResultUnit())
                        .referenceRange(i.getReferenceRange())
                        .abnormal(i.isAbnormal())
                        .resultNotes(i.getResultNotes())
                        .resultedAt(i.getResultedAt())
                        .build())
            .collect(Collectors.toList());

    return LabRequestResponse.builder()
        .id(r.getId())
        .patientId(r.getPatient().getId())
        .patientName(r.getPatient().getFullName())
        .episodeId(r.getEpisode() != null ? r.getEpisode().getId() : null)
        .requestedById(r.getRequestedBy() != null ? r.getRequestedBy().getId() : null)
        .requestedByName(r.getRequestedBy() != null ? r.getRequestedBy().getFullName() : null)
        .status(r.getStatus())
        .priority(r.getPriority())
        .clinicalNotes(r.getClinicalNotes())
        .collectedAt(r.getCollectedAt())
        .completedAt(r.getCompletedAt())
        .createdAt(r.getCreatedAt())
        .items(items)
        .build();
  }
}
