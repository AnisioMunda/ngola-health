package ao.hospitalao.modules.triage.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.episodes.entity.Episode;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.entity.TenantScopedEntity;
import ao.hospitalao.modules.patients.entity.Patient;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "triage_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriageRecord extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false, insertable = false, updatable = false)
  private Hospital hospital;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id")
  private Patient patient;

  @Column(name = "queue_number", nullable = false)
  private Integer queueNumber;

  @Column(name = "patient_name_temp", length = 200)
  private String patientNameTemp;

  @Column(name = "patient_age_temp")
  private Integer patientAgeTemp;

  @Column(name = "patient_gender_temp", length = 10)
  private String patientGenderTemp;

  @Column(name = "priority", nullable = false, length = 10)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private TriagePriority priority = TriagePriority.GREEN;

  @Column(name = "chief_complaint", nullable = false, length = 500)
  private String chiefComplaint;

  @Column(name = "blood_pressure", length = 20)
  private String bloodPressure;

  @Column(name = "heart_rate")
  private Integer heartRate;

  @Column(name = "temperature", precision = 4, scale = 1)
  private BigDecimal temperature;

  @Column(name = "oxygen_saturation")
  private Integer oxygenSaturation;

  @Column(name = "respiratory_rate")
  private Integer respiratoryRate;

  @Column(name = "weight_kg", precision = 5, scale = 2)
  private BigDecimal weightKg;

  @Column(name = "pain_scale")
  private Integer painScale;

  @Column(name = "triage_notes", columnDefinition = "TEXT")
  private String triageNotes;

  @Column(name = "status", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private TriageStatus status = TriageStatus.WAITING;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "triaged_by")
  private User triagedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "episode_id")
  private Episode episode;

  @Column(name = "triaged_at", nullable = false)
  private OffsetDateTime triagedAt;

  @Column(name = "attended_at")
  private OffsetDateTime attendedAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

  @PrePersist
  protected void onCreate() {
    if (this.triagedAt == null) this.triagedAt = OffsetDateTime.now();
  }

  /** Minutos em espera */
  public long getWaitingMinutes() {
    OffsetDateTime ref = attendedAt != null ? attendedAt : OffsetDateTime.now();
    return ChronoUnit.MINUTES.between(triagedAt, ref);
  }

  /** Verificar se está a exceder o tempo máximo de espera para a prioridade */
  public boolean isOverdue() {
    if (status != TriageStatus.WAITING) return false;
    long maxWait =
        switch (priority) {
          case RED -> 0L;
          case ORANGE -> 10L;
          case YELLOW -> 60L;
          case GREEN -> 120L;
          case BLUE -> 240L;
        };
    return getWaitingMinutes() > maxWait;
  }

  /** Nome para exibição — paciente registado ou temporário */
  public String getDisplayName() {
    if (patient != null) return patient.getFullName();
    return patientNameTemp != null ? patientNameTemp : "Paciente #" + queueNumber;
  }

  public enum TriagePriority {
    RED,
    ORANGE,
    YELLOW,
    GREEN,
    BLUE
  }

  public enum TriageStatus {
    WAITING,
    IN_PROGRESS,
    COMPLETED,
    TRANSFERRED,
    LEFT
  }
}
