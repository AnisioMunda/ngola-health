package ao.hospitalao.modules.pharmacy.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.hospitals.entity.TenantScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "stock_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockBatch extends TenantScopedEntity {

  private static final ZoneId ANGOLA_ZONE = ZoneId.of("Africa/Luanda");

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "medication_id", nullable = false)
  private Medication medication;

  @Column(name = "batch_number", nullable = false, length = 50)
  private String batchNumber;

  @Column(name = "expiry_date", nullable = false)
  private LocalDate expiryDate;

  @Column(name = "quantity_received", nullable = false)
  private Integer quantityReceived;

  @Column(name = "quantity_available", nullable = false)
  private Integer quantityAvailable;

  @Column(name = "unit_cost", precision = 10, scale = 2)
  private BigDecimal unitCost;

  @Column(name = "supplier", length = 200)
  private String supplier;

  @Column(name = "received_at", nullable = false)
  private OffsetDateTime receivedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @PrePersist
  protected void onCreate() {
    this.receivedAt = OffsetDateTime.now();
    if (this.quantityAvailable == null) {
      this.quantityAvailable = this.quantityReceived;
    }
  }

  public boolean isExpired() {
    return expiryDate != null && expiryDate.isBefore(LocalDate.now(ANGOLA_ZONE));
  }

  public boolean isExpiringWithin(int days) {
    LocalDate today = LocalDate.now(ANGOLA_ZONE);
    return expiryDate != null
        && !expiryDate.isBefore(today)
        && !expiryDate.isAfter(today.plusDays(days));
  }
}
