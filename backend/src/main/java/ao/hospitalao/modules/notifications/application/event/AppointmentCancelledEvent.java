package ao.hospitalao.modules.notifications.application.event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentCancelledEvent(
    UUID hospitalId,
    UUID appointmentId,
    UUID doctorId,
    String patientName,
    LocalDate appointmentDate,
    LocalTime startTime) {}
