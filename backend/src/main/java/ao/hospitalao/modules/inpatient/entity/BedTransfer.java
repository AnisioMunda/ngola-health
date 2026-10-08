package ao.hospitalao.modules.inpatient.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "bed_transfers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BedTransfer extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "admission_id", nullable = false)
  private Admission admission;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_bed_id", nullable = false)
  private Bed fromBed;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "to_bed_id", nullable = false)
  private Bed toBed;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_ward_id", nullable = false)
  private Ward fromWard;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "to_ward_id", nullable = false)
  private Ward toWard;

  @Column(name = "reason", length = 300)
  private String reason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transferred_by")
  private User transferredBy;

  @Column(name = "transferred_at", nullable = false)
  private OffsetDateTime transferredAt;

  @PrePersist
  protected void onCreate() {
    if (this.transferredAt == null) this.transferredAt = OffsetDateTime.now();
  }
}
