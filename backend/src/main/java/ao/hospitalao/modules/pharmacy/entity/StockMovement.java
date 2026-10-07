package ao.hospitalao.modules.pharmacy.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.episodes.entity.Episode;
import ao.hospitalao.modules.hospitals.entity.TenantScopedEntity;
import ao.hospitalao.modules.patients.entity.Patient;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "stock_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "batch_id", nullable = false)
  private StockBatch batch;

  @Column(name = "movement_type", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private MovementType movementType;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id")
  private Patient patient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "episode_id")
  private Episode episode;

  @Column(name = "reason", length = 500)
  private String reason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "performed_by")
  private User performedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
  }

  public enum MovementType {
    IN,
    DISPENSE,
    ADJUSTMENT,
    EXPIRED,
    RETURNED
  }
}
