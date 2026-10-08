package ao.hospitalao.modules.equipment.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "maintenance_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecord extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "equipment_id", nullable = false)
  private Equipment equipment;

  @Column(name = "type", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  private MaintenanceType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "performed_by")
  private User performedBy;

  @Column(name = "performed_at", nullable = false)
  private OffsetDateTime performedAt;

  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "cost", precision = 10, scale = 2)
  private BigDecimal cost;

  @Column(name = "next_maintenance_date")
  private LocalDate nextMaintenanceDate;

  @Column(name = "parts_replaced", length = 500)
  private String partsReplaced;

  @Column(name = "result", length = 20)
  @Builder.Default
  private String result = "OK";

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (this.performedAt == null) this.performedAt = OffsetDateTime.now();
    this.createdAt = OffsetDateTime.now();
  }

  public enum MaintenanceType {
    PREVENTIVE,
    CORRECTIVE,
    CALIBRATION,
    INSPECTION
  }
}
