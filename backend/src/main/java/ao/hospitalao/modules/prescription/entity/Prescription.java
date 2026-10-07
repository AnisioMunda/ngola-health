package ao.hospitalao.modules.prescription.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.episodes.entity.Episode;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.entity.TenantScopedEntity;
import ao.hospitalao.modules.inpatient.entity.Admission;
import ao.hospitalao.modules.patients.entity.Patient;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "prescriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription extends TenantScopedEntity {

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
  @JoinColumn(name = "episode_id")
  private Episode episode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "admission_id")
  private Admission admission;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "doctor_id", nullable = false)
  private User doctor;

  @Column(name = "status", nullable = false, length = 25)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private PrescriptionStatus status = PrescriptionStatus.ACTIVE;

  @Column(name = "prescription_date", nullable = false)
  private LocalDate prescriptionDate;

  @Column(name = "expiry_date", nullable = false)
  private LocalDate expiryDate;

  @Column(name = "diagnosis", length = 500)
  private String diagnosis;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "prescription_number", nullable = false, unique = true, length = 30)
  private String prescriptionNumber;

  @Column(name = "cancelled_reason", length = 300)
  private String cancelledReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cancelled_by")
  private User cancelledBy;

  @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<PrescriptionItem> items = new ArrayList<>();

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

  public boolean isExpired() {
    return expiryDate != null && LocalDate.now().isAfter(expiryDate);
  }

  public enum PrescriptionStatus {
    ACTIVE,
    PARTIALLY_DISPENSED,
    DISPENSED,
    CANCELLED,
    EXPIRED
  }
}
