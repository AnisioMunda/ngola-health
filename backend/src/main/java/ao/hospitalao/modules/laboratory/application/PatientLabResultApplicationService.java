package ao.hospitalao.modules.laboratory.application;

import ao.hospitalao.modules.laboratory.entity.LabRequest.RequestStatus;
import ao.hospitalao.modules.laboratory.repository.LabRequestRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientLabResultApplicationService {

  private static final List<RequestStatus> PENDING_STATUSES =
      List.of(RequestStatus.PENDING, RequestStatus.COLLECTED, RequestStatus.IN_ANALYSIS);

  private final LabRequestRepository requestRepository;

  @Transactional(readOnly = true)
  public List<PatientLabResult> findCompletedResults(UUID patientId) {
    return requestRepository
        .findPatientResults(patientId, RequestStatus.COMPLETED, PageRequest.of(0, 50))
        .stream()
        .map(this::toPatientLabResult)
        .toList();
  }

  @Transactional(readOnly = true)
  public long countPendingResults(UUID patientId) {
    return requestRepository.countByPatientIdAndStatusIn(patientId, PENDING_STATUSES);
  }

  private PatientLabResult toPatientLabResult(PatientLabResultProjection resultProjection) {
    String referenceValues =
        resultProjection.getReferenceRange() == null
                || resultProjection.getReferenceRange().isBlank()
            ? resultProjection.getTestReferenceValues()
            : resultProjection.getReferenceRange();
    String result =
        resultProjection.getResultUnit() == null || resultProjection.getResultUnit().isBlank()
            ? resultProjection.getResultValue()
            : resultProjection.getResultValue() + " " + resultProjection.getResultUnit();

    return new PatientLabResult(
        resultProjection.getId(),
        resultProjection.getExamName(),
        resultProjection.getStatus().name(),
        result,
        referenceValues,
        resultProjection.getDoctorName(),
        resultProjection.getRequestedAt(),
        resultProjection.getResultAt());
  }

  public record PatientLabResult(
      UUID id,
      String examName,
      String status,
      String result,
      String referenceValues,
      String doctorName,
      OffsetDateTime requestedAt,
      OffsetDateTime resultAt) {}
}
