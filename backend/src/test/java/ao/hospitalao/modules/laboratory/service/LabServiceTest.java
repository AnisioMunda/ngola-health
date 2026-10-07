package ao.hospitalao.modules.laboratory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.entity.Episode;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.laboratory.dto.LabDtos.CreateLabRequestRequest;
import ao.hospitalao.modules.laboratory.dto.LabDtos.CreateLabTestRequest;
import ao.hospitalao.modules.laboratory.dto.LabDtos.SubmitResultRequest;
import ao.hospitalao.modules.laboratory.entity.LabRequest;
import ao.hospitalao.modules.laboratory.entity.LabRequest.RequestStatus;
import ao.hospitalao.modules.laboratory.entity.LabRequestItem;
import ao.hospitalao.modules.laboratory.entity.LabTest;
import ao.hospitalao.modules.laboratory.entity.LabTest.TestCategory;
import ao.hospitalao.modules.laboratory.repository.LabRequestRepository;
import ao.hospitalao.modules.laboratory.repository.LabTestRepository;
import ao.hospitalao.modules.notifications.event.LabResultsAvailableEvent;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class LabServiceTest {

  @Mock private LabRequestRepository requestRepository;
  @Mock private LabTestRepository testRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private EpisodeRepository episodeRepository;
  @Mock private UserRepository userRepository;
  @Mock private HospitalRepository hospitalRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private LabService labService;

  private UUID hospitalId;
  private UUID patientId;
  private UUID testId;
  private UUID requestId;
  private User currentUser;
  private Patient patient;
  private LabTest labTest;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
    patientId = UUID.randomUUID();
    testId = UUID.randomUUID();
    requestId = UUID.randomUUID();
    TenantContext.setCurrentHospital(hospitalId);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "medica", "password", List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))));

    currentUser =
        User.builder().id(UUID.randomUUID()).fullName("Médica").username("medica").build();
    currentUser.setHospitalId(hospitalId);
    patient = Patient.builder().id(patientId).fullName("Ana Silva").build();
    patient.setHospitalId(hospitalId);
    labTest =
        LabTest.builder()
            .id(testId)
            .code("HEM-01")
            .name("Hemograma")
            .category(TestCategory.HEMATOLOGY)
            .build();
    labTest.setHospitalId(hospitalId);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void createTestTrimsAndNormalizesCodeBeforeCheckingDuplicates() {
    var request = new CreateLabTestRequest();
    request.setCode(" hem-01 ");
    request.setName(" Hemograma ");
    request.setCategory(TestCategory.HEMATOLOGY);
    when(testRepository.existsByHospitalIdAndCode(hospitalId, "HEM-01")).thenReturn(false);
    when(testRepository.save(any(LabTest.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = labService.createTest(request);

    assertThat(response.getCode()).isEqualTo("HEM-01");
    assertThat(response.getName()).isEqualTo("Hemograma");
  }

  @Test
  void createRequestDefaultsRequesterAndAddsRequestedTests() {
    var request = new CreateLabRequestRequest();
    request.setPatientId(patientId);
    request.setLabTestIds(List.of(testId));
    when(userRepository.findByUsername("medica")).thenReturn(Optional.of(currentUser));
    when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
    when(testRepository.findById(testId)).thenReturn(Optional.of(labTest));
    when(requestRepository.save(any(LabRequest.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = labService.create(request);

    assertThat(response.getStatus()).isEqualTo(RequestStatus.PENDING);
    assertThat(response.getRequestedById()).isEqualTo(currentUser.getId());
    assertThat(response.getItems()).hasSize(1);
    assertThat(response.getItems().get(0).getLabTestId()).isEqualTo(testId);
  }

  @Test
  void createRequestRejectsEpisodeForAnotherPatient() {
    UUID episodeId = UUID.randomUUID();
    Patient otherPatient =
        Patient.builder().id(UUID.randomUUID()).fullName("Outro paciente").build();
    otherPatient.setHospitalId(hospitalId);
    Episode episode = Episode.builder().id(episodeId).patient(otherPatient).build();
    episode.setHospitalId(hospitalId);
    var request = new CreateLabRequestRequest();
    request.setPatientId(patientId);
    request.setEpisodeId(episodeId);
    request.setLabTestIds(List.of(testId));
    when(userRepository.findByUsername("medica")).thenReturn(Optional.of(currentUser));
    when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
    when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));

    var exception = assertThrows(ResponseStatusException.class, () -> labService.create(request));

    assertThat(exception.getStatusCode().value()).isEqualTo(400);
    verify(requestRepository, never()).save(any(LabRequest.class));
  }

  @Test
  void createRequestRejectsInactiveTests() {
    labTest.setActive(false);
    var request = new CreateLabRequestRequest();
    request.setPatientId(patientId);
    request.setLabTestIds(List.of(testId));
    when(userRepository.findByUsername("medica")).thenReturn(Optional.of(currentUser));
    when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
    when(testRepository.findById(testId)).thenReturn(Optional.of(labTest));

    var exception = assertThrows(ResponseStatusException.class, () -> labService.create(request));

    assertThat(exception.getStatusCode().value()).isEqualTo(400);
    verify(requestRepository, never()).save(any(LabRequest.class));
  }

  @Test
  void laboratoryCatalogRequiresHospitalScope() {
    TenantContext.setPlatformAccess();

    var exception = assertThrows(ResponseStatusException.class, labService::findAllTests);

    assertThat(exception.getStatusCode().value()).isEqualTo(403);
    verifyNoInteractions(testRepository);
  }

  @Test
  void collectOnlyAllowsPendingRequests() {
    LabRequest request = requestWithStatus(RequestStatus.COMPLETED);
    when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

    var exception =
        assertThrows(ResponseStatusException.class, () -> labService.collect(requestId));

    assertThat(exception.getStatusCode().value()).isEqualTo(409);
    verify(requestRepository, never()).save(any(LabRequest.class));
  }

  @Test
  void collectionMovesPendingRequestToCollected() {
    LabRequest request = requestWithStatus(RequestStatus.PENDING);
    when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
    when(requestRepository.save(request)).thenReturn(request);

    var response = labService.collect(requestId);

    assertThat(response.getStatus()).isEqualTo(RequestStatus.COLLECTED);
    assertThat(response.getCollectedAt()).isNotNull();
  }

  @Test
  void analysisCannotStartBeforeCollection() {
    LabRequest request = requestWithStatus(RequestStatus.PENDING);
    when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

    var exception =
        assertThrows(ResponseStatusException.class, () -> labService.startAnalysis(requestId));

    assertThat(exception.getStatusCode().value()).isEqualTo(409);
    verify(requestRepository, never()).save(any(LabRequest.class));
  }

  @Test
  void submittingLastResultCompletesRequest() {
    LabRequest request = requestWithStatus(RequestStatus.IN_ANALYSIS);
    LabRequestItem item =
        LabRequestItem.builder().id(UUID.randomUUID()).request(request).labTest(labTest).build();
    request.getItems().add(item);
    var result = new SubmitResultRequest();
    result.setResultValue("Normal");
    when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
    when(userRepository.findByUsername("medica")).thenReturn(Optional.of(currentUser));
    when(requestRepository.save(request)).thenReturn(request);

    var response = labService.submitItemResult(requestId, item.getId(), result);

    assertThat(response.getStatus()).isEqualTo(RequestStatus.COMPLETED);
    assertThat(response.getCompletedAt()).isNotNull();
    assertThat(item.getResultedBy()).isEqualTo(currentUser);
    verify(eventPublisher)
        .publishEvent(new LabResultsAvailableEvent(hospitalId, requestId, currentUser.getId()));
  }

  @Test
  void resultCannotBeOverwritten() {
    LabRequest request = requestWithStatus(RequestStatus.IN_ANALYSIS);
    LabRequestItem item =
        LabRequestItem.builder()
            .id(UUID.randomUUID())
            .request(request)
            .labTest(labTest)
            .resultValue("Anterior")
            .build();
    request.getItems().add(item);
    var result = new SubmitResultRequest();
    result.setResultValue("Novo");
    when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

    var exception =
        assertThrows(
            ResponseStatusException.class,
            () -> labService.submitItemResult(requestId, item.getId(), result));

    assertThat(exception.getStatusCode().value()).isEqualTo(409);
    assertThat(item.getResultValue()).isEqualTo("Anterior");
    verify(requestRepository, never()).save(any(LabRequest.class));
  }

  private LabRequest requestWithStatus(RequestStatus status) {
    LabRequest request =
        LabRequest.builder()
            .id(requestId)
            .patient(patient)
            .requestedBy(currentUser)
            .createdBy(currentUser)
            .status(status)
            .items(new ArrayList<>())
            .build();
    request.setHospitalId(hospitalId);
    return request;
  }
}
