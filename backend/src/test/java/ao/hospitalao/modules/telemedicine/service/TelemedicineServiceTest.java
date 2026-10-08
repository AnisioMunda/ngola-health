package ao.hospitalao.modules.telemedicine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.application.UserApplicationService;
import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.scheduling.application.TelemedicineAppointmentApplicationService;
import ao.hospitalao.modules.telemedicine.dto.TelemedicineDtos.CreateSessionRequest;
import ao.hospitalao.modules.telemedicine.dto.TelemedicineDtos.UpdateNotesRequest;
import ao.hospitalao.modules.telemedicine.entity.TelemedicineSession;
import ao.hospitalao.modules.telemedicine.provider.TeamsMeetingProvider;
import ao.hospitalao.modules.telemedicine.provider.TeamsMeetingProviderException;
import ao.hospitalao.modules.telemedicine.repository.TelemedicineRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TelemedicineServiceTest {

  @Mock private TelemedicineRepository telemedicineRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private UserRepository userRepository;
  @Mock private HospitalRepository hospitalRepository;
  @Mock private TeamsMeetingProvider teamsMeetingProvider;
  @Mock private UserApplicationService userApplicationService;
  @Mock private TelemedicineAppointmentApplicationService appointmentApplicationService;

  @InjectMocks private TelemedicineService service;

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void persistsTheTeamsMeetingAndSelectedOrganizerWithConfiguredDuration() {
    UUID hospitalId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    UUID organizerId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    OffsetDateTime scheduledAt = OffsetDateTime.now().plusDays(1);
    String joinUrl = "https://teams.microsoft.com/l/meetup-join/meeting";
    TenantContext.setCurrentHospital(hospitalId);

    Patient patient = Patient.builder().id(patientId).fullName("Patient Test").build();
    patient.setHospitalId(hospitalId);
    User doctor = User.builder().id(doctorId).fullName("Dr. Test").build();
    when(patientRepository.getReferenceById(patientId)).thenReturn(patient);
    when(userApplicationService.findTeamsDoctor(doctorId, hospitalId))
        .thenReturn(
            new UserApplicationService.TeamsDoctor(doctorId, "Dr. Test", organizerId.toString()));
    when(userRepository.getReferenceById(doctorId)).thenReturn(doctor);
    when(teamsMeetingProvider.createMeeting(scheduledAt, 45, organizerId.toString()))
        .thenReturn(new TeamsMeetingProvider.TeamsMeeting(meetingId.toString(), joinUrl));
    when(telemedicineRepository.saveAndFlush(any(TelemedicineSession.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreateSessionRequest request = new CreateSessionRequest();
    request.setPatientId(patientId);
    request.setDoctorId(doctorId);
    request.setScheduledAt(scheduledAt);
    request.setDurationMinutes(45);

    var response = service.create(request);

    ArgumentCaptor<TelemedicineSession> sessionCaptor =
        ArgumentCaptor.forClass(TelemedicineSession.class);
    verify(telemedicineRepository).saveAndFlush(sessionCaptor.capture());
    TelemedicineSession saved = sessionCaptor.getValue();
    assertThat(saved.getProviderMeetingId()).isEqualTo(meetingId.toString());
    assertThat(saved.getProviderOrganizerId()).isEqualTo(organizerId);
    assertThat(saved.getRoomUrl()).isEqualTo(joinUrl);
    assertThat(saved.getDurationMinutes()).isEqualTo(45);
    assertThat(saved.getDoctor().getId()).isEqualTo(doctorId);
    assertThat(response.getRoomUrl()).isEqualTo(joinUrl);
    assertThat(response.getDoctorId()).isEqualTo(doctorId);
  }

  @Test
  void rejectsPatientsFromAnotherHospitalBeforeCreatingAnExternalMeeting() {
    UUID hospitalId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    TenantContext.setCurrentHospital(hospitalId);

    Patient patient = Patient.builder().id(patientId).build();
    patient.setHospitalId(UUID.randomUUID());
    when(patientRepository.getReferenceById(patientId)).thenReturn(patient);

    CreateSessionRequest request = new CreateSessionRequest();
    request.setPatientId(patientId);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    verify(teamsMeetingProvider, org.mockito.Mockito.never())
        .createMeeting(any(), org.mockito.ArgumentMatchers.anyInt(), any());
  }

  @Test
  void revokesTheTeamsMeetingIfTheSessionCannotBePersisted() {
    UUID hospitalId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    UUID organizerId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    OffsetDateTime scheduledAt = OffsetDateTime.now().plusDays(1);
    TenantContext.setCurrentHospital(hospitalId);

    Patient patient = Patient.builder().id(patientId).fullName("Patient Test").build();
    patient.setHospitalId(hospitalId);
    when(patientRepository.getReferenceById(patientId)).thenReturn(patient);
    when(userApplicationService.findTeamsDoctor(doctorId, hospitalId))
        .thenReturn(
            new UserApplicationService.TeamsDoctor(doctorId, "Dr. Test", organizerId.toString()));
    when(teamsMeetingProvider.createMeeting(scheduledAt, 30, organizerId.toString()))
        .thenReturn(
            new TeamsMeetingProvider.TeamsMeeting(
                meetingId.toString(), "https://teams.microsoft.com/l/meetup-join/meeting"));
    org.mockito.Mockito.doThrow(new DataIntegrityViolationException("database unavailable"))
        .when(telemedicineRepository)
        .saveAndFlush(any(TelemedicineSession.class));
    CreateSessionRequest request = new CreateSessionRequest();
    request.setPatientId(patientId);
    request.setDoctorId(doctorId);
    request.setScheduledAt(scheduledAt);
    request.setDurationMinutes(30);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(teamsMeetingProvider).deleteMeeting(meetingId.toString(), organizerId.toString());
  }

  @Test
  void revokesTheTeamsMeetingBeforeCancellingAndClearsTheStoredJoinUrl() {
    UUID sessionId = UUID.randomUUID();
    UUID organizerId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    TelemedicineSession session =
        TelemedicineSession.builder()
            .id(sessionId)
            .patient(Patient.builder().id(patientId).fullName("Patient Test").build())
            .doctor(User.builder().id(doctorId).fullName("Dr. Test").build())
            .providerMeetingId("meeting-id")
            .providerOrganizerId(organizerId)
            .roomUrl("https://teams.microsoft.com/l/meetup-join/meeting")
            .scheduledAt(OffsetDateTime.now().plusDays(1))
            .build();
    when(telemedicineRepository.findById(sessionId)).thenReturn(java.util.Optional.of(session));
    when(telemedicineRepository.save(session)).thenReturn(session);

    var response = service.cancel(sessionId);

    verify(teamsMeetingProvider).deleteMeeting("meeting-id", organizerId.toString());
    assertThat(session.getStatus()).isEqualTo(TelemedicineSession.SessionStatus.CANCELLED);
    assertThat(session.getRoomUrl()).isNull();
    assertThat(response.getRoomUrl()).isNull();
  }

  @Test
  void revokesTheTeamsMeetingWhenEndingTheSession() {
    UUID sessionId = UUID.randomUUID();
    UUID organizerId = UUID.randomUUID();
    TelemedicineSession session =
        TelemedicineSession.builder()
            .id(sessionId)
            .patient(Patient.builder().id(UUID.randomUUID()).fullName("Patient Test").build())
            .doctor(User.builder().id(UUID.randomUUID()).fullName("Dr. Test").build())
            .providerMeetingId("meeting-id")
            .providerOrganizerId(organizerId)
            .roomUrl("https://teams.microsoft.com/l/meetup-join/meeting")
            .scheduledAt(OffsetDateTime.now().plusDays(1))
            .build();
    when(telemedicineRepository.findById(sessionId)).thenReturn(java.util.Optional.of(session));
    when(telemedicineRepository.save(session)).thenReturn(session);
    UpdateNotesRequest notes = new UpdateNotesRequest();
    notes.setClinicalNotes("Consulta concluída.");

    var response = service.endSession(sessionId, notes);

    verify(teamsMeetingProvider).deleteMeeting("meeting-id", organizerId.toString());
    assertThat(session.getStatus()).isEqualTo(TelemedicineSession.SessionStatus.COMPLETED);
    assertThat(session.getRoomUrl()).isNull();
    assertThat(response.getClinicalNotes()).isEqualTo("Consulta concluída.");
    assertThat(response.getRoomUrl()).isNull();
  }

  @Test
  void doesNotCancelTheLocalSessionWhenTeamsRevocationFails() {
    UUID sessionId = UUID.randomUUID();
    UUID organizerId = UUID.randomUUID();
    TelemedicineSession session =
        TelemedicineSession.builder()
            .id(sessionId)
            .patient(Patient.builder().id(UUID.randomUUID()).fullName("Patient Test").build())
            .doctor(User.builder().id(UUID.randomUUID()).fullName("Dr. Test").build())
            .providerMeetingId("meeting-id")
            .providerOrganizerId(organizerId)
            .roomUrl("https://teams.microsoft.com/l/meetup-join/meeting")
            .scheduledAt(OffsetDateTime.now().plusDays(1))
            .build();
    when(telemedicineRepository.findById(sessionId)).thenReturn(java.util.Optional.of(session));
    org.mockito.Mockito.doThrow(new TeamsMeetingProviderException("Microsoft Graph unavailable"))
        .when(teamsMeetingProvider)
        .deleteMeeting("meeting-id", organizerId.toString());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.cancel(sessionId))
        .isInstanceOf(TeamsMeetingProviderException.class);
    assertThat(session.getStatus()).isEqualTo(TelemedicineSession.SessionStatus.SCHEDULED);
    assertThat(session.getRoomUrl()).isEqualTo("https://teams.microsoft.com/l/meetup-join/meeting");
    org.mockito.Mockito.verify(telemedicineRepository, org.mockito.Mockito.never()).save(session);
  }
}
