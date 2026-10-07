package ao.hospitalao.modules.notifications.service;

import static org.mockito.Mockito.verify;

import ao.hospitalao.modules.notifications.event.AppointmentCancelledEvent;
import ao.hospitalao.modules.notifications.event.LabResultsAvailableEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

  @Mock private NotificationService notificationService;

  @InjectMocks private NotificationEventListener listener;

  @Test
  void labResultEventCreatesNotificationForRequester() {
    UUID hospitalId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    listener.onLabResultsAvailable(new LabResultsAvailableEvent(hospitalId, requestId, userId));

    verify(notificationService).notifyLabResult(hospitalId, requestId, userId);
  }

  @Test
  void appointmentCancellationEventCreatesFormattedNotification() {
    UUID hospitalId = UUID.randomUUID();
    UUID appointmentId = UUID.randomUUID();
    UUID doctorId = UUID.randomUUID();

    listener.onAppointmentCancelled(
        new AppointmentCancelledEvent(
            hospitalId,
            appointmentId,
            doctorId,
            "Ana Silva",
            LocalDate.of(2026, 5, 21),
            LocalTime.of(10, 30)));

    verify(notificationService)
        .notifyAppointmentCancelled(
            hospitalId, doctorId, "Ana Silva", LocalDate.of(2026, 5, 21), "10:30", appointmentId);
  }
}
