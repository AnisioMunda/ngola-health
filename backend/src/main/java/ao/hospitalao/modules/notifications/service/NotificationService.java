package ao.hospitalao.modules.notifications.service;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.notifications.entity.Notification;
import ao.hospitalao.modules.notifications.entity.Notification.NotificationType;
import ao.hospitalao.modules.notifications.entity.Notification.Priority;
import ao.hospitalao.modules.notifications.repository.NotificationRepository;
import ao.hospitalao.modules.pharmacy.repository.MedicationRepository;
import ao.hospitalao.modules.pharmacy.repository.StockBatchRepository;
import ao.hospitalao.modules.scheduling.entity.Appointment;
import ao.hospitalao.modules.scheduling.repository.AppointmentRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final MedicationRepository medicationRepository;
  private final StockBatchRepository stockBatchRepository;
  private final InvoiceRepository invoiceRepository;
  private final AppointmentRepository appointmentRepository;
  private final HospitalRepository hospitalRepository;

  // ------------------------------------------------
  // Queries para o utilizador actual
  // ------------------------------------------------

  @Transactional(readOnly = true)
  public Page<NotificationDto> getForUser(UUID hospitalId, UUID userId, Pageable pageable) {
    return notificationRepository.findForUser(hospitalId, userId, pageable).map(this::toDto);
  }

  @Transactional(readOnly = true)
  public long countUnread(UUID hospitalId, UUID userId) {
    return notificationRepository.countUnread(hospitalId, userId);
  }

  @Transactional(readOnly = true)
  public List<NotificationDto> getTopUnread(UUID hospitalId, UUID userId) {
    return notificationRepository.findTopUnread(hospitalId, userId, PageRequest.of(0, 5)).stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public void markAsRead(UUID id) {
    notificationRepository
        .findById(id)
        .ifPresent(
            n -> {
              n.setRead(true);
              n.setReadAt(OffsetDateTime.now());
              notificationRepository.save(n);
            });
  }

  @Transactional
  public void markAllAsRead(UUID hospitalId, UUID userId) {
    int count = notificationRepository.markAllAsRead(hospitalId, userId);
    log.info("Marked {} notifications as read for user {}", count, userId);
  }

  // ------------------------------------------------
  // Criação manual de notificações
  // ------------------------------------------------

  @Transactional
  public void create(
      UUID hospitalId,
      UUID userId,
      NotificationType type,
      Priority priority,
      String title,
      String message,
      String actionUrl,
      UUID referenceId,
      String referenceType) {

    // Evitar duplicados nas últimas 24h
    if (referenceId != null
        && notificationRepository.existsRecentNotification(
            hospitalId, type, referenceId, OffsetDateTime.now().minusHours(24))) {
      return;
    }

    Notification notification =
        Notification.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .user(userId != null ? User.builder().id(userId).build() : null)
            .type(type)
            .priority(priority)
            .title(title)
            .message(message)
            .actionUrl(actionUrl)
            .referenceId(referenceId)
            .referenceType(referenceType)
            .build();

    notificationRepository.save(notification);
    log.info("Notification created: [{}] {} for hospital {}", type, title, hospitalId);
  }

  // ------------------------------------------------
  // Triggers automáticos — chamados por outros services
  // ------------------------------------------------

  /** Chamado quando um resultado de exame é submetido */
  @Transactional
  public void notifyLabResult(
      UUID hospitalId, UUID labRequestId, String patientName, UUID doctorId) {
    create(
        hospitalId,
        doctorId,
        NotificationType.LAB_RESULT,
        Priority.HIGH,
        "Resultado de Exame Disponível",
        "O resultado do pedido de " + patientName + " está pronto para revisão.",
        "/lab/" + labRequestId,
        labRequestId,
        "LAB_REQUEST");
  }

  /** Chamado quando uma consulta é cancelada */
  @Transactional
  public void notifyAppointmentCancelled(
      UUID hospitalId,
      UUID doctorId,
      String patientName,
      LocalDate date,
      String time,
      UUID appointmentId) {
    create(
        hospitalId,
        doctorId,
        NotificationType.APPOINTMENT_CANCELLED,
        Priority.HIGH,
        "Consulta Cancelada",
        patientName + " cancelou a consulta de " + date + " às " + time + ".",
        "/scheduling/" + appointmentId,
        appointmentId,
        "APPOINTMENT");
  }

  /** Chamado quando uma factura fica em atraso */
  @Transactional
  public void notifyInvoiceOverdue(
      UUID hospitalId, UUID invoiceId, String invoiceNumber, String patientName) {
    create(
        hospitalId,
        null, // null = todos os utilizadores do hospital
        NotificationType.INVOICE_OVERDUE,
        Priority.HIGH,
        "Factura em Atraso",
        "A factura " + invoiceNumber + " de " + patientName + " ultrapassou a data de vencimento.",
        "/financial/" + invoiceId,
        invoiceId,
        "INVOICE");
  }

  // ------------------------------------------------
  // Jobs agendados — verificações automáticas
  // ------------------------------------------------

  /** Verificar stock baixo — executa todos os dias às 8h00. */
  @Scheduled(cron = "0 0 8 * * *")
  @Transactional
  public void checkLowStock() {
    log.info("Running low stock notification check...");
    LocalDate today = LocalDate.now();

    hospitalRepository
        .findAll()
        .forEach(
            hospital -> {
              UUID hospitalId = hospital.getId();

              medicationRepository
                  .findByHospitalIdAndActiveTrue(hospitalId)
                  .forEach(
                      med -> {
                        int available =
                            Optional.ofNullable(
                                    stockBatchRepository.getTotalAvailableQuantity(
                                        med.getId(), today))
                                .orElse(0);

                        if (available <= med.getMinStockLevel()) {
                          Priority priority = available == 0 ? Priority.CRITICAL : Priority.HIGH;
                          String title =
                              available == 0
                                  ? "Stock Esgotado: " + med.getName()
                                  : "Stock Baixo: " + med.getName();
                          String msg =
                              available == 0
                                  ? med.getName() + " está sem stock. Reposição urgente necessária."
                                  : med.getName()
                                      + " tem apenas "
                                      + available
                                      + " unidades (mínimo: "
                                      + med.getMinStockLevel()
                                      + ").";

                          create(
                              hospitalId,
                              null,
                              NotificationType.LOW_STOCK,
                              priority,
                              title,
                              msg,
                              "/pharmacy/" + med.getId(),
                              med.getId(),
                              "MEDICATION");
                        }
                      });
            });
  }

  /** Verificar lotes a expirar nos próximos 30 dias — executa todos os dias às 8h30. */
  @Scheduled(cron = "0 30 8 * * *")
  @Transactional
  public void checkExpiringStock() {
    log.info("Running expiring stock notification check...");
    LocalDate today = LocalDate.now();
    LocalDate in30Days = today.plusDays(30);

    hospitalRepository
        .findAll()
        .forEach(
            hospital -> {
              UUID hospitalId = hospital.getId();
              stockBatchRepository
                  .findExpiringSoon(hospitalId, today, in30Days)
                  .forEach(
                      batch -> {
                        long daysLeft =
                            java.time.temporal.ChronoUnit.DAYS.between(
                                today, batch.getExpiryDate());
                        Priority priority = daysLeft <= 7 ? Priority.CRITICAL : Priority.HIGH;

                        create(
                            hospitalId,
                            null,
                            NotificationType.EXPIRING_STOCK,
                            priority,
                            "Lote a Expirar: " + batch.getMedication().getName(),
                            "O lote "
                                + batch.getBatchNumber()
                                + " de "
                                + batch.getMedication().getName()
                                + " expira em "
                                + daysLeft
                                + " dia(s) ("
                                + batch.getExpiryDate()
                                + ")."
                                + " Quantidade disponível: "
                                + batch.getQuantityAvailable()
                                + " unidades.",
                            "/pharmacy/" + batch.getMedication().getId(),
                            batch.getId(),
                            "STOCK_BATCH");
                      });
            });
  }

  /** Verificar facturas em atraso — executa todos os dias às 9h00. */
  @Scheduled(cron = "0 0 9 * * *")
  @Transactional
  public void checkOverdueInvoices() {
    log.info("Running overdue invoice notification check...");
    LocalDate today = LocalDate.now();

    hospitalRepository
        .findAll()
        .forEach(
            hospital -> {
              invoiceRepository.findTop5ByHospitalIdOrderByCreatedAtDesc(hospital.getId()).stream()
                  .filter(
                      inv ->
                          inv.getStatus() == Invoice.InvoiceStatus.EMITIDO
                              || inv.getStatus() == Invoice.InvoiceStatus.PAGO_PARCIALMENTE)
                  .filter(inv -> inv.getDueDate() != null && inv.getDueDate().isBefore(today))
                  .forEach(
                      inv ->
                          notifyInvoiceOverdue(
                              hospital.getId(), inv.getId(),
                              inv.getInvoiceNumber(), inv.getPatient().getFullName()));
            });
  }

  /** Lembrete de consultas do dia seguinte — executa todos os dias às 18h00. */
  @Scheduled(cron = "0 0 18 * * *")
  @Transactional
  public void checkTomorrowAppointments() {
    log.info("Running tomorrow appointments reminder check...");
    LocalDate tomorrow = LocalDate.now().plusDays(1);

    hospitalRepository
        .findAll()
        .forEach(
            hospital -> {
              appointmentRepository
                  .findByHospitalAndDateRange(hospital.getId(), tomorrow, tomorrow)
                  .stream()
                  .filter(
                      a ->
                          a.getStatus() == Appointment.AppointmentStatus.SCHEDULED
                              || a.getStatus() == Appointment.AppointmentStatus.CONFIRMED)
                  .forEach(
                      a ->
                          create(
                              hospital.getId(),
                              a.getDoctor().getId(),
                              NotificationType.APPOINTMENT,
                              Priority.MEDIUM,
                              "Consulta Amanhã: " + a.getPatient().getFullName(),
                              a.getPatient().getFullName()
                                  + " tem consulta amanhã às "
                                  + a.getStartTime()
                                  + ".",
                              "/scheduling/" + a.getId(),
                              a.getId(),
                              "APPOINTMENT"));
            });
  }

  // ------------------------------------------------
  // Mapper
  // ------------------------------------------------

  private NotificationDto toDto(Notification n) {
    return NotificationDto.builder()
        .id(n.getId())
        .type(n.getType())
        .priority(n.getPriority())
        .title(n.getTitle())
        .message(n.getMessage())
        .actionUrl(n.getActionUrl())
        .referenceId(n.getReferenceId())
        .referenceType(n.getReferenceType())
        .read(n.isRead())
        .readAt(n.getReadAt())
        .createdAt(n.getCreatedAt())
        .build();
  }

  // ------------------------------------------------
  // DTO inline
  // ------------------------------------------------

  @lombok.Data
  @lombok.Builder
  public static class NotificationDto {
    private UUID id;
    private NotificationType type;
    private Priority priority;
    private String title;
    private String message;
    private String actionUrl;
    private UUID referenceId;
    private String referenceType;
    private boolean read;
    private OffsetDateTime readAt;
    private OffsetDateTime createdAt;
  }
}
