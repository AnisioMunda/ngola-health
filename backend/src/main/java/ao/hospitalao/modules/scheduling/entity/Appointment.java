package ao.hospitalao.modules.scheduling.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.episodes.entity.Episode;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.entity.TenantScopedEntity;
import ao.hospitalao.modules.patients.entity.Patient;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false, insertable = false, updatable = false)
  private Hospital hospital;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "doctor_id", nullable = false)
  private User doctor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "episode_id")
  private Episode episode;

  @Column(name = "appointment_date", nullable = false)
  private LocalDate appointmentDate;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Column(name = "slot_position", nullable = false)
  @Builder.Default
  private int slotPosition = 1;

  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private AppointmentStatus status = AppointmentStatus.SCHEDULED;

  @Column(name = "appointment_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private AppointmentType appointmentType = AppointmentType.OUTPATIENT;

  @Column(name = "reason", nullable = false, length = 300)
  private String reason;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "cancellation_reason", length = 300)
  private String cancellationReason;

  @Column(name = "cancelled_at")
  private OffsetDateTime cancelledAt;

  @Column(name = "confirmed_at")
  private OffsetDateTime confirmedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booked_by")
  private User bookedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
    this.updatedAt = OffsetDateTime.now();
  }

  public Appointment(UUID id) {
    this.id = id;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = OffsetDateTime.now();
  }

  public enum AppointmentStatus {
    SCHEDULED, // Agendado
    CONFIRMED, // Confirmado pelo paciente
    COMPLETED, // Consulta realizada
    CANCELLED, // Cancelado
    NO_SHOW // Paciente não compareceu
  }

  public enum AppointmentType {
    OUTPATIENT, // Consulta ambulatório
    EMERGENCY, // Urgência
    EXAM, // Exame
    SURGERY, // Cirurgia
    FOLLOW_UP // Consulta de seguimento
  }
}
