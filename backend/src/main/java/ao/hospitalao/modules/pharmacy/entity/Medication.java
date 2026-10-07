package ao.hospitalao.modules.pharmacy.entity;

import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.entity.TenantScopedEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "medications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false, insertable = false, updatable = false)
  private Hospital hospital;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "generic_name", length = 200)
  private String genericName;

  @Column(name = "dosage_form", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private DosageForm dosageForm;

  @Column(name = "strength", length = 50)
  private String strength;

  @Column(name = "unit", nullable = false, length = 20)
  private String unit;

  @Column(name = "requires_prescription", nullable = false)
  @Builder.Default
  private boolean requiresPrescription = true;

  @Column(name = "min_stock_level")
  @Builder.Default
  private Integer minStockLevel = 10;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
  }

  public enum DosageForm {
    TABLET,
    CAPSULE,
    SYRUP,
    INJECTION,
    CREAM,
    OINTMENT,
    DROPS,
    INHALER,
    OTHER
  }
}
