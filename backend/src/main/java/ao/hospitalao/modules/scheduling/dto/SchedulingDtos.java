package ao.hospitalao.modules.scheduling.dto;

import ao.hospitalao.modules.scheduling.entity.Appointment.AppointmentStatus;
import ao.hospitalao.modules.scheduling.entity.Appointment.AppointmentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

public class SchedulingDtos {

  // ============================================================
  // Horário do médico
  // ============================================================

  @Data
  @Builder
  public static class DoctorScheduleResponse {
    private UUID id;
    private UUID doctorId;
    private String doctorName;
    private String specialty;
    private int dayOfWeek;
    private String dayLabel;
    private String startTime;
    private String endTime;
    private int slotDurationMinutes;
    private int maxPatientsPerSlot;
    private boolean active;
  }

  @Data
  public static class CreateScheduleRequest {
    @NotNull private UUID doctorId;

    @NotNull
    @Min(0)
    @Max(6)
    private Integer dayOfWeek;

    @NotNull private LocalTime startTime;

    @NotNull private LocalTime endTime;

    @Min(1)
    private Integer slotDurationMinutes;

    @Min(1)
    private Integer maxPatientsPerSlot;
  }

  // ============================================================
  // Slots disponíveis
  // ============================================================

  @Data
  @Builder
  public static class SlotResponse {
    private LocalDate date;
    private String dateLabel;
    private LocalTime startTime;
    private LocalTime endTime;
    private UUID doctorId;
    private String doctorName;
    private boolean available;
    private int bookedCount;
    private int maxPatients;
  }

  @Data
  @Builder
  public static class DayAvailabilityResponse {
    private LocalDate date;
    private String dateLabel;
    private String dayOfWeek;
    private List<SlotResponse> slots;
    private int totalSlots;
    private int availableSlots;
  }

  // ============================================================
  // Agendamentos
  // ============================================================

  @Data
  @Builder
  public static class AppointmentResponse {
    private UUID id;
    private UUID patientId;
    private String patientName;
    private String patientPhone;
    private UUID doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private LocalDate appointmentDate;
    private String appointmentDateLabel;
    private LocalTime startTime;
    private LocalTime endTime;
    private AppointmentStatus status;
    private String statusLabel;
    private AppointmentType appointmentType;
    private String reason;
    private String notes;
    private String cancellationReason;
    private String bookedByName;
    private String createdAt;
    private String confirmedAt;
  }

  @Data
  public static class CreateAppointmentRequest {
    private UUID patientId;
    private UUID doctorId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private AppointmentType appointmentType;
    private String reason;
    private String notes;
  }

  @Data
  public static class CancelAppointmentRequest {
    private String reason;
  }

  // ============================================================
  // Calendário — visão semanal/mensal
  // ============================================================

  @Data
  @Builder
  public static class CalendarEventResponse {
    private UUID id;
    private String title;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String patientName;
    private String doctorName;
    private AppointmentStatus status;
    private String color;
  }

  // ============================================================
  // Bloqueio de agenda
  // ============================================================

  @Data
  public static class CreateBlockRequest {
    @NotNull private UUID doctorId;

    @NotNull private LocalDate blockDate;

    private LocalTime startTime;
    private LocalTime endTime;
    private boolean allDay;
    private String reason;
  }
}
