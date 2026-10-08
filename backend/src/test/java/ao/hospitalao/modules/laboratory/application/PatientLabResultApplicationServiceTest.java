package ao.hospitalao.modules.laboratory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.laboratory.entity.LabRequest.RequestStatus;
import ao.hospitalao.modules.laboratory.repository.LabRequestRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PatientLabResultApplicationServiceTest {

  @Mock private LabRequestRepository requestRepository;

  @InjectMocks private PatientLabResultApplicationService service;

  @Test
  void mapsCompletedResultsAndUsesTestReferenceValuesWhenRequestRangeIsBlank() {
    UUID patientId = UUID.randomUUID();
    UUID resultId = UUID.randomUUID();
    OffsetDateTime requestedAt = OffsetDateTime.now().minusDays(1);
    OffsetDateTime resultAt = OffsetDateTime.now();
    PatientLabResultProjection projection = mock(PatientLabResultProjection.class);
    when(projection.getId()).thenReturn(resultId);
    when(projection.getExamName()).thenReturn("Hemoglobina");
    when(projection.getStatus()).thenReturn(RequestStatus.COMPLETED);
    when(projection.getResultValue()).thenReturn("13.2");
    when(projection.getResultUnit()).thenReturn("g/dL");
    when(projection.getReferenceRange()).thenReturn(" ");
    when(projection.getTestReferenceValues()).thenReturn("12 - 16 g/dL");
    when(projection.getDoctorName()).thenReturn("Dr. Test");
    when(projection.getRequestedAt()).thenReturn(requestedAt);
    when(projection.getResultAt()).thenReturn(resultAt);
    when(requestRepository.findPatientResults(
            eq(patientId), eq(RequestStatus.COMPLETED), eq(PageRequest.of(0, 50))))
        .thenReturn(new PageImpl<>(List.of(projection)));

    var results = service.findCompletedResults(patientId);

    assertThat(results)
        .containsExactly(
            new PatientLabResultApplicationService.PatientLabResult(
                resultId,
                "Hemoglobina",
                "COMPLETED",
                "13.2 g/dL",
                "12 - 16 g/dL",
                "Dr. Test",
                requestedAt,
                resultAt));
  }

  @Test
  void countsOnlyPendingLaboratoryStatuses() {
    UUID patientId = UUID.randomUUID();
    when(requestRepository.countByPatientIdAndStatusIn(eq(patientId), anyList())).thenReturn(2L);

    assertThat(service.countPendingResults(patientId)).isEqualTo(2);

    verify(requestRepository)
        .countByPatientIdAndStatusIn(
            patientId,
            List.of(RequestStatus.PENDING, RequestStatus.COLLECTED, RequestStatus.IN_ANALYSIS));
  }
}
