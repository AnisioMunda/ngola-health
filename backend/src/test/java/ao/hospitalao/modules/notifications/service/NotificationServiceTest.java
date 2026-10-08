package ao.hospitalao.modules.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.notifications.entity.Notification;
import ao.hospitalao.modules.notifications.entity.Notification.NotificationType;
import ao.hospitalao.modules.notifications.repository.NotificationRepository;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.pharmacy.repository.MedicationRepository;
import ao.hospitalao.modules.pharmacy.repository.StockBatchRepository;
import ao.hospitalao.modules.scheduling.entity.Appointment;
import ao.hospitalao.modules.scheduling.entity.Appointment.AppointmentStatus;
import ao.hospitalao.modules.scheduling.repository.AppointmentRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  private static final ZoneId ANGOLA_ZONE = ZoneId.of("Africa/Luanda");

  @Mock private NotificationRepository notificationRepository;
  @Mock private MedicationRepository medicationRepository;
  @Mock private StockBatchRepository stockBatchRepository;
  @Mock private InvoiceRepository invoiceRepository;
  @Mock private AppointmentRepository appointmentRepository;
  @Mock private HospitalRepository hospitalRepository;

  @InjectMocks private NotificationService notificationService;

  @Test
  void markAsReadUpdatesOnlyNotificationVisibleToCurrentUser() {
    UUID notificationId = UUID.randomUUID();
    UUID hospitalId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Notification notification = Notification.builder().id(notificationId).build();
    when(notificationRepository.findVisibleToUser(notificationId, hospitalId, userId))
        .thenReturn(Optional.of(notification));

    notificationService.markAsRead(notificationId, hospitalId, userId);

    assertThat(notification.isRead()).isTrue();
    assertThat(notification.getReadAt()).isNotNull();
    verify(notificationRepository).save(notification);
  }

  @Test
  void markAsReadReturnsNotFoundForAnotherUsersNotification() {
    UUID notificationId = UUID.randomUUID();
    UUID hospitalId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(notificationRepository.findVisibleToUser(notificationId, hospitalId, userId))
        .thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> notificationService.markAsRead(notificationId, hospitalId, userId));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    verify(notificationRepository, never()).save(any(Notification.class));
  }

  @Test
  void tomorrowReminderNotifiesDoctorForScheduledAppointmentsOnly() {
    UUID hospitalId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    LocalDate tomorrow = LocalDate.now(ANGOLA_ZONE).plusDays(1);
    Hospital hospital = Hospital.builder().id(hospitalId).build();
    Appointment scheduled = appointment(tomorrow, AppointmentStatus.SCHEDULED, doctorId, "Ana");
    Appointment confirmed = appointment(tomorrow, AppointmentStatus.CONFIRMED, doctorId, "Beto");
    Appointment cancelled = appointment(tomorrow, AppointmentStatus.CANCELLED, doctorId, "Cris");
    when(hospitalRepository.findAll()).thenReturn(List.of(hospital));
    when(appointmentRepository.findByHospitalAndDateRange(hospitalId, tomorrow, tomorrow))
        .thenReturn(List.of(scheduled, cancelled, confirmed));

    notificationService.checkTomorrowAppointments();

    ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository, times(2)).save(notificationCaptor.capture());
    assertThat(notificationCaptor.getAllValues())
        .extracting(Notification::getType)
        .containsExactly(NotificationType.APPOINTMENT, NotificationType.APPOINTMENT);
    assertThat(notificationCaptor.getAllValues())
        .extracting(notification -> notification.getUser().getId())
        .containsExactly(doctorId, doctorId);
    assertThat(notificationCaptor.getAllValues())
        .extracting(Notification::getReferenceId)
        .containsExactly(scheduled.getId(), confirmed.getId());
  }

  @Test
  void tomorrowReminderDoesNotRecreateAnExistingAppointmentNotification() {
    UUID hospitalId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();
    LocalDate tomorrow = LocalDate.now(ANGOLA_ZONE).plusDays(1);
    Appointment appointment = appointment(tomorrow, AppointmentStatus.SCHEDULED, doctorId, "Ana");
    when(hospitalRepository.findAll())
        .thenReturn(List.of(Hospital.builder().id(hospitalId).build()));
    when(appointmentRepository.findByHospitalAndDateRange(hospitalId, tomorrow, tomorrow))
        .thenReturn(List.of(appointment));
    when(notificationRepository.existsRecentNotification(
            eq(hospitalId),
            eq(NotificationType.APPOINTMENT),
            eq(appointment.getId()),
            any(OffsetDateTime.class)))
        .thenReturn(true);

    notificationService.checkTomorrowAppointments();

    verify(notificationRepository, never()).save(any(Notification.class));
  }

  private Appointment appointment(
      LocalDate date, AppointmentStatus status, UUID doctorId, String patientName) {
    return Appointment.builder()
        .id(UUID.randomUUID())
        .doctor(User.builder().id(doctorId).fullName("Médico").build())
        .patient(Patient.builder().id(UUID.randomUUID()).fullName(patientName).build())
        .appointmentDate(date)
        .startTime(LocalTime.of(10, 0))
        .status(status)
        .build();
  }
}
