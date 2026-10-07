package ao.hospitalao.modules.inpatient.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.episodes.entity.Episode;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.patients.entity.Patient;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "admissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admission {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false)
  private Hospital hospital;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "episode_id")
  private Episode episode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bed_id", nullable = false)
  private Bed bed;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ward_id", nullable = false)
  private Ward ward;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "responsible_doctor_id", nullable = false)
  private User responsibleDoctor;

  @Column(name = "status", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private AdmissionStatus status = AdmissionStatus.ACTIVE;

  @Column(name = "admission_date", nullable = false)
  private OffsetDateTime admissionDate;

  @Column(name = "expected_discharge_date")
  private LocalDate expectedDischargeDate;

  @Column(name = "discharge_date")
  private OffsetDateTime dischargeDate;

  @Column(name = "admission_reason", nullable = false, length = 500)
  private String admissionReason;

  @Column(name = "diagnosis", columnDefinition = "TEXT")
  private String diagnosis;

  @Column(name = "discharge_notes", columnDefinition = "TEXT")
  private String dischargeNotes;

  @Column(name = "discharge_condition", length = 30)
  @Enumerated(EnumType.STRING)
  private DischargeCondition dischargeCondition;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "admitted_by")
  private User admittedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "discharged_by")
  private User dischargedBy;

  @OneToMany(mappedBy = "admission", cascade = CascadeType.ALL)
  @Builder.Default
  private List<BedTransfer> transfers = new ArrayList<>();

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

  public long getDaysAdmitted() {
    OffsetDateTime end = dischargeDate != null ? dischargeDate : OffsetDateTime.now();
    return java.time.temporal.ChronoUnit.DAYS.between(
        admissionDate.toLocalDate(), end.toLocalDate());
  }

  public enum AdmissionStatus {
    ACTIVE,
    DISCHARGED,
    TRANSFERRED,
    DECEASED
  }

  public enum DischargeCondition {
    IMPROVED,
    STABLE,
    CRITICAL,
    DECEASED,
    AGAINST_MEDICAL_ADVICE
  }
}
