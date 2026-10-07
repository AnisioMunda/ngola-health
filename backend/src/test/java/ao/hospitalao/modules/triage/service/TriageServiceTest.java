package ao.hospitalao.modules.triage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.triage.dto.TriageDtos.TriageResponse;
import ao.hospitalao.modules.triage.entity.TriageRecord;
import ao.hospitalao.modules.triage.entity.TriageRecord.TriagePriority;
import ao.hospitalao.modules.triage.entity.TriageRecord.TriageStatus;
import ao.hospitalao.modules.triage.repository.TriageRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TriageServiceTest {

  @Mock private TriageRepository triageRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private UserRepository userRepository;
  @Mock private HospitalRepository hospitalRepository;

  @InjectMocks private TriageService triageService;

  @ParameterizedTest
  @EnumSource(TriagePriority.class)
  void exposesManchesterLabelColourAndTargetWait(TriagePriority priority) {
    ManchesterTarget target = targetFor(priority);
    TriageRecord record =
        TriageRecord.builder()
            .id(UUID.randomUUID())
            .queueNumber(1)
            .priority(priority)
            .status(TriageStatus.WAITING)
            .chiefComplaint("Queixa")
            .triagedAt(OffsetDateTime.now().minusMinutes(target.minutes() + 1))
            .build();
    when(triageRepository.findById(record.getId())).thenReturn(Optional.of(record));

    TriageResponse response = triageService.findById(record.getId());

    assertEquals(target.label(), response.getPriorityLabel());
    assertEquals(target.colour(), response.getPriorityColor());
    assertTrue(response.isOverdue());
  }

  @ParameterizedTest
  @EnumSource(TriagePriority.class)
  void doesNotMarkWaitingPatientOverdueBeforeTarget(TriagePriority priority) {
    ManchesterTarget target = targetFor(priority);
    TriageRecord record =
        TriageRecord.builder()
            .id(UUID.randomUUID())
            .queueNumber(1)
            .priority(priority)
            .status(TriageStatus.WAITING)
            .chiefComplaint("Queixa")
            .triagedAt(OffsetDateTime.now().minusMinutes(target.minutes()))
            .build();

    assertFalse(record.isOverdue());
  }

  @Test
  void callingWaitingPatientStartsAttendanceClock() {
    UUID id = UUID.randomUUID();
    TriageRecord record = waitingRecord(id);
    when(triageRepository.findById(id)).thenReturn(Optional.of(record));
    when(triageRepository.save(record)).thenReturn(record);

    TriageResponse response = triageService.callNext(id);

    assertEquals(TriageStatus.IN_PROGRESS, response.getStatus());
    assertNotNull(record.getAttendedAt());
  }

  @Test
  void completingWaitingPatientIsRejected() {
    UUID id = UUID.randomUUID();
    TriageRecord record = waitingRecord(id);
    when(triageRepository.findById(id)).thenReturn(Optional.of(record));

    assertThrows(IllegalStateException.class, () -> triageService.complete(id));

    verify(triageRepository, never()).save(record);
  }

  private TriageRecord waitingRecord(UUID id) {
    return TriageRecord.builder()
        .id(id)
        .queueNumber(1)
        .priority(TriagePriority.GREEN)
        .status(TriageStatus.WAITING)
        .chiefComplaint("Queixa")
        .triagedAt(OffsetDateTime.now())
        .build();
  }

  private ManchesterTarget targetFor(TriagePriority priority) {
    return switch (priority) {
      case RED -> new ManchesterTarget("Imediato", "#dc2626", 0);
      case ORANGE -> new ManchesterTarget("Muito Urgente", "#ea580c", 10);
      case YELLOW -> new ManchesterTarget("Urgente", "#ca8a04", 60);
      case GREEN -> new ManchesterTarget("Pouco Urgente", "#16a34a", 120);
      case BLUE -> new ManchesterTarget("Não Urgente", "#2563eb", 240);
    };
  }

  private record ManchesterTarget(String label, String colour, int minutes) {}
}
