package ao.hospitalao.modules.laboratory.service;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        UUID hospitalId = TenantContext.getCurrentHospital();
        return testRepository.findByHospitalIdAndActiveTrue(hospitalId)
            .stream().map(this::toTestResponse).collect(Collectors.toList());
    }

    @SuppressWarnings("null")
    @Transactional
    public LabTestResponse createTest(CreateLabTestRequest req) {
        UUID hospitalId = TenantContext.getCurrentHospital();
        if (testRepository.existsByHospitalIdAndCode(hospitalId, req.getCode())) {
            throw new IllegalArgumentException("Test code already exists: " + req.getCode());
        }

        LabTest test = LabTest.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .code(req.getCode().toUpperCase())
            .name(req.getName())
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
        UUID hospitalId = TenantContext.getCurrentHospital();
        return requestRepository.findWithFilters(hospitalId, patientId, status, pageable)
            .map(this::toRequestResponse);
    }

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public LabRequestResponse findById(UUID id) {
        return requestRepository.findById(id)
            .map(this::toRequestResponse)
            .orElseThrow(() -> new EntityNotFoundException("Lab request not found: " + id));
    }

    @SuppressWarnings("null")
    @Transactional
    public LabRequestResponse create(CreateLabRequestRequest req) {
        UUID hospitalId = TenantContext.getCurrentHospital();

        var patient = patientRepository.findById(req.getPatientId())
            .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        var request = LabRequest.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .patient(patient)
            .status(RequestStatus.PENDING)
            .priority(req.getPriority() != null ? req.getPriority() : LabRequest.Priority.NORMAL)
            .clinicalNotes(req.getClinicalNotes())
            .createdBy(getCurrentUser())
            .build();

        if (req.getEpisodeId() != null) {
            request.setEpisode(episodeRepository.getReferenceById(req.getEpisodeId()));
        }
        if (req.getRequestedById() != null) {
            request.setRequestedBy(userRepository.getReferenceById(req.getRequestedById()));
        }

        // Adicionar itens
        if (req.getLabTestIds() != null) {
            for (UUID testId : req.getLabTestIds()) {
                LabTest test = testRepository.findById(testId)
                    .orElseThrow(() -> new EntityNotFoundException("Lab test not found: " + testId));
                LabRequestItem item = LabRequestItem.builder()
                    .request(request)
                    .labTest(test)
                    .build();
                request.getItems().add(item);
            }
        }

        LabRequest saved = requestRepository.save(request);
        log.info("Lab request created: {} for patient {}", saved.getId(), patient.getFullName());
        return toRequestResponse(saved);
    }

    @Transactional
    public LabRequestResponse collect(UUID id) {
        LabRequest request = getOrThrow(id);
        request.setStatus(RequestStatus.COLLECTED);
        request.setCollectedAt(OffsetDateTime.now());
        return toRequestResponse(requestRepository.save(request));
    }

    @Transactional
    public LabRequestResponse startAnalysis(UUID id) {
        LabRequest request = getOrThrow(id);
        request.setStatus(RequestStatus.IN_ANALYSIS);
        return toRequestResponse(requestRepository.save(request));
    }

    @Transactional
    public LabRequestResponse submitItemResult(UUID requestId, UUID itemId, SubmitResultRequest req) {
        LabRequest request = getOrThrow(requestId);

        LabRequestItem item = request.getItems().stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemId));

        item.setResultValue(req.getResultValue());
        item.setResultUnit(req.getResultUnit());
        item.setReferenceRange(req.getReferenceRange());
        item.setAbnormal(req.isAbnormal());
        item.setResultNotes(req.getResultNotes());
        item.setResultedAt(OffsetDateTime.now());
        item.setResultedBy(getCurrentUser());

        // Se todos os itens têm resultado, completar o pedido
        boolean allResulted = request.getItems().stream()
            .allMatch(i -> i.getResultValue() != null);
        if (allResulted) {
            request.setStatus(RequestStatus.COMPLETED);
            request.setCompletedAt(OffsetDateTime.now());
        }

        return toRequestResponse(requestRepository.save(request));
    }

    @Transactional
    public LabRequestResponse cancel(UUID id) {
        LabRequest request = getOrThrow(id);
        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed request.");
        }
        request.setStatus(RequestStatus.CANCELLED);
        return toRequestResponse(requestRepository.save(request));
    }

    // ------------------------------------------------
    // Helpers
    // ------------------------------------------------

    @SuppressWarnings("null")
    private LabRequest getOrThrow(UUID id) {
        return requestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Lab request not found: " + id));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private LabTestResponse toTestResponse(LabTest t) {
        return LabTestResponse.builder()
            .id(t.getId()).code(t.getCode()).name(t.getName())
            .category(t.getCategory()).sampleType(t.getSampleType())
            .turnaroundHours(t.getTurnaroundHours()).price(t.getPrice())
            .referenceValues(t.getReferenceValues()).active(t.isActive())
            .build();
    }

    private LabRequestResponse toRequestResponse(LabRequest r) {
        List<LabRequestItemResponse> items = r.getItems().stream()
            .map(i -> LabRequestItemResponse.builder()
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