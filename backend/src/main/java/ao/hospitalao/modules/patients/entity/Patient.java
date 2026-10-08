package ao.hospitalao.modules.patients.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
    name = "patients",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_patients_hospital_national_id",
          columnNames = {"hospital_id", "national_id"}),
      @UniqueConstraint(
          name = "uq_patients_hospital_health_card",
          columnNames = {"hospital_id", "health_card_number"})
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @Column(name = "full_name", nullable = false, length = 200)
  private String fullName;

  @Column(name = "birth_date", nullable = false)
  private LocalDate birthDate;

  // EnumType.STRING guarda "MALE"/"FEMALE" como VARCHAR
  // após a migration 002-fix-patients-gender.xml
  @Column(name = "gender", nullable = false, length = 10)
  @Enumerated(EnumType.STRING)
  private Gender gender;

  @Column(name = "national_id", length = 20)
  private String nationalId;

  @Column(name = "health_card_number", length = 30)
  private String healthCardNumber;

  @Column(name = "phone", length = 20)
  private String phone;

  @Column(name = "email", length = 200)
  private String email;

  @Column(name = "address", length = 500)
  private String address;

  @Column(name = "province", length = 100)
  private String province;

  @Column(name = "municipality", length = 100)
  private String municipality;

  @Column(name = "emergency_contact_name", length = 200)
  private String emergencyContactName;

  @Column(name = "emergency_contact_phone", length = 20)
  private String emergencyContactPhone;

  @Column(name = "emergency_contact_relationship", length = 50)
  private String emergencyContactRelationship;

  @Column(name = "blood_type", length = 5)
  private String bloodType;

  @Column(name = "allergies", columnDefinition = "TEXT")
  private String allergies;

  @Column(name = "chronic_conditions", columnDefinition = "TEXT")
  private String chronicConditions;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private boolean active = true;

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

  public enum Gender {
    MALE,
    FEMALE
  }
}
