package ao.hospitalao.modules.portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

public class PortalDtos {

  // ============================================================
  // Auth
  // ============================================================

  @Data
  public static class PortalRegisterRequest {
    @NotBlank
    @Email
    @Size(max = 200)
    private String email;

    @NotBlank
    @Size(min = 12, max = 72)
    private String password;

    @NotBlank
    @Size(max = 30)
    private String patientNumber; // número de processo ou NIF
  }

  @Data
  public static class PortalLoginRequest {
    @NotBlank
    @Email
    @Size(max = 200)
    private String email;

    @NotBlank private String password;
  }

  @Data
  @Builder
  public static class PortalRegistrationResponse {
    private String status;
    private String message;
  }

  @Data
  @Builder
  public static class PortalPendingAccountDto {
    private UUID accountId;
    private UUID patientId;
    private String patientName;
    private String email;
    private OffsetDateTime requestedAt;
  }

  @Data
  @Builder
  public static class PortalLoginResponse {
    private String token;
    private String patientName;
    private UUID patientId;
    private String email;
  }

  // ============================================================
  // Perfil do paciente
  // ============================================================

  @Data
  @Builder
  public static class PatientProfileDto {
    private UUID id;
    private String fullName;
    private String email;
    private LocalDate birthDate;
    private String gender;
    private String phone;
    private String address;
    private String nif;
    private String patientNumber;
    private boolean emailVerified;
    private OffsetDateTime lastLoginAt;
  }

  // ============================================================
  // Consultas / episódios
  // ============================================================

  @Data
  @Builder
  public static class PortalEpisodeDto {
    private UUID id;
    private String episodeType;
    private String status;
    private String statusLabel;
    private String doctorName;
    private OffsetDateTime scheduledAt;
    private OffsetDateTime completedAt;
    private String reason;
    private String diagnosis;
    private boolean hasLabResults;
    private boolean hasPrescriptions;
  }

  // ============================================================
  // Resultados laboratoriais
  // ============================================================

  @Data
  @Builder
  public static class PortalLabResultDto {
    private UUID id;
    private String examName;
    private String status;
    private String result;
    private String referenceValues;
    private String doctorName;
    private OffsetDateTime requestedAt;
    private OffsetDateTime resultAt;
  }

  // ============================================================
  // Prescrições
  // ============================================================

  @Data
  @Builder
  public static class PortalPrescriptionDto {
    private UUID id;
    private String prescriptionNumber;
    private String doctorName;
    private LocalDate prescriptionDate;
    private LocalDate expiryDate;
    private String status;
    private String statusLabel;
    private String diagnosis;
    private List<PortalPrescriptionItemDto> items;
  }

  @Data
  @Builder
  public static class PortalPrescriptionItemDto {
    private String medicationName;
    private String dosage;
    private String route;
    private int quantityPrescribed;
    private int quantityDispensed;
    private String status;
  }

  // ============================================================
  // Facturas
  // ============================================================

  @Data
  @Builder
  public static class PortalInvoiceDto {
    private UUID id;
    private String invoiceNumber;
    private OffsetDateTime issueDate;
    private BigDecimal totalAmount;
    private String status;
    private String statusLabel;
    private String description;
  }

  // ============================================================
  // Agendamento online
  // ============================================================

  @Data
  public static class PortalBookAppointmentRequest {
    private UUID doctorId;
    private String scheduledAt;
    private String reason;
  }

  @Data
  @Builder
  public static class PortalDashboardDto {
    private String patientName;
    private int totalEpisodes;
    private int upcomingAppointments;
    private int pendingLabResults;
    private int pendingInvoices;
    private BigDecimal totalDebt;
    private List<PortalEpisodeDto> recentEpisodes;
    private List<PortalAppointmentDto> upcomingAppointmentsList;
  }

  @Data
  @Builder
  public static class PortalAppointmentDto {
    private UUID id;
    private String doctorName;
    private String specialty;
    private OffsetDateTime scheduledAt;
    private String status;
    private String reason;
  }
}
