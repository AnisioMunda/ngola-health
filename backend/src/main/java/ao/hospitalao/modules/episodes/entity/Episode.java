package ao.hospitalao.modules.episodes.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "episodes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Episode extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @Column(name = "patient_id", insertable = false, updatable = false)
  private UUID patientId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false, insertable = false, updatable = false)
  private Hospital hospital;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "doctor_id")
  private User doctor;

  @Column(name = "episode_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private EpisodeType episodeType;

  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private EpisodeStatus status = EpisodeStatus.SCHEDULED;

  // Scheduling
  @Column(name = "scheduled_at")
  private OffsetDateTime scheduledAt;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

  // Clinical record
  @Column(name = "reason", length = 500)
  private String reason;

  @Column(name = "symptoms", columnDefinition = "TEXT")
  private String symptoms;

  @Column(name = "diagnosis", columnDefinition = "TEXT")
  private String diagnosis;

  @Column(name = "prescription", columnDefinition = "TEXT")
  private String prescription;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  // Vitals
  @Column(name = "blood_pressure", length = 20)
  private String bloodPressure;

  @Column(name = "heart_rate")
  private Integer heartRate;

  @Column(name = "temperature", precision = 4, scale = 1)
  private BigDecimal temperature;

  @Column(name = "weight_kg", precision = 5, scale = 2)
  private BigDecimal weightKg;

  // Audit
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
    this.updatedAt = OffsetDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = OffsetDateTime.now();
  }

  public enum EpisodeType {
    EMERGENCY,
    OUTPATIENT,
    INPATIENT,
    OUTPATIENT_SURGERY,
    EXAM
  }

  public enum EpisodeStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
  }
}
