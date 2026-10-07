package ao.hospitalao.modules.notifications.service;

import ao.hospitalao.modules.notifications.event.AppointmentCancelledEvent;
import ao.hospitalao.modules.notifications.event.LabResultsAvailableEvent;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private static final DateTimeFormatter APPOINTMENT_TIME = DateTimeFormatter.ofPattern("HH:mm");

  private final NotificationService notificationService;

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  public void onLabResultsAvailable(LabResultsAvailableEvent event) {
    notificationService.notifyLabResult(
        event.hospitalId(), event.labRequestId(), event.recipientUserId());
  }

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  public void onAppointmentCancelled(AppointmentCancelledEvent event) {
    notificationService.notifyAppointmentCancelled(
        event.hospitalId(),
        event.doctorId(),
        event.patientName(),
        event.appointmentDate(),
        event.startTime().format(APPOINTMENT_TIME),
        event.appointmentId());
  }
}
