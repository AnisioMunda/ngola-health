package ao.hospitalao.modules.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.notifications.entity.Notification;
import ao.hospitalao.modules.notifications.repository.NotificationRepository;
import ao.hospitalao.modules.pharmacy.repository.MedicationRepository;
import ao.hospitalao.modules.pharmacy.repository.StockBatchRepository;
import ao.hospitalao.modules.scheduling.repository.AppointmentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

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
}
