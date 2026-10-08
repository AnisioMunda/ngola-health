package ao.hospitalao.modules.telemedicine.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.scheduling.entity.Appointment;
import ao.hospitalao.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "telemedicine_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemedicineSession extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false, insertable = false, updatable = false)
  private Hospital hospital;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "appointment_id")
  private Appointment appointment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "doctor_id", nullable = false)
  private User doctor;

  @Column(name = "status", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private SessionStatus status = SessionStatus.SCHEDULED;

  @Column(name = "room_token", nullable = false, unique = true)
  private String roomToken;

  @Column(name = "scheduled_at", nullable = false)
  private OffsetDateTime scheduledAt;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "ended_at")
  private OffsetDateTime endedAt;

  @Column(name = "duration_minutes")
  private Integer durationMinutes;

  @Column(name = "clinical_notes", columnDefinition = "TEXT")
  private String clinicalNotes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
  }

  public void calculateDuration() {
    if (startedAt != null && endedAt != null) {
      this.durationMinutes = (int) ChronoUnit.MINUTES.between(startedAt, endedAt);
    }
  }

  public enum SessionStatus {
    SCHEDULED,
    WAITING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW
  }
}
