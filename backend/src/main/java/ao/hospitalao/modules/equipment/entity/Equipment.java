package ao.hospitalao.modules.equipment.entity;

import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.inpatient.entity.Ward;
import ao.hospitalao.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "equipment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false, insertable = false, updatable = false)
  private Hospital hospital;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "code", nullable = false, unique = true, length = 50)
  private String code;

  @Column(name = "brand", length = 100)
  private String brand;

  @Column(name = "model", length = 100)
  private String model;

  @Column(name = "serial_number", length = 100)
  private String serialNumber;

  @Column(name = "category", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private EquipmentCategory category = EquipmentCategory.OTHER;

  @Column(name = "location", length = 200)
  private String location;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ward_id")
  private Ward ward;

  @Column(name = "status", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private EquipmentStatus status = EquipmentStatus.ACTIVE;

  @Column(name = "purchase_date")
  private LocalDate purchaseDate;

  @Column(name = "purchase_price", precision = 12, scale = 2)
  private BigDecimal purchasePrice;

  @Column(name = "warranty_expiry")
  private LocalDate warrantyExpiry;

  @Column(name = "next_maintenance_date")
  private LocalDate nextMaintenanceDate;

  @Column(name = "maintenance_interval_days")
  @Builder.Default
  private Integer maintenanceIntervalDays = 365;

  @Column(name = "next_calibration_date")
  private LocalDate nextCalibrationDate;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL)
  @Builder.Default
  private List<MaintenanceRecord> maintenanceRecords = new ArrayList<>();

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

  public boolean isMaintenanceDue() {
    return nextMaintenanceDate != null && !LocalDate.now().isBefore(nextMaintenanceDate);
  }

  public boolean isWarrantyExpired() {
    return warrantyExpiry != null && LocalDate.now().isAfter(warrantyExpiry);
  }

  public enum EquipmentCategory {
    DIAGNOSTIC,
    THERAPEUTIC,
    SURGICAL,
    MONITORING,
    MOBILITY,
    IT,
    OTHER
  }

  public enum EquipmentStatus {
    ACTIVE,
    MAINTENANCE,
    REPAIR,
    RETIRED,
    RESERVED
  }
}
