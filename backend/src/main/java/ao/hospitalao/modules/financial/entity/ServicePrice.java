package ao.hospitalao.modules.financial.entity;

import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "service_prices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicePrice extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false, insertable = false, updatable = false)
  private Hospital hospital;

  @Column(name = "code", nullable = false, length = 30)
  private String code;

  @Column(name = "description", nullable = false, length = 300)
  private String description;

  @Column(name = "category", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private ServiceCategory category;

  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal vatRate = BigDecimal.ZERO;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
  }

  public enum ServiceCategory {
    CONSULTATION,
    EXAM,
    PROCEDURE,
    SURGERY,
    ACCOMMODATION,
    PHARMACY,
    LAB,
    IMAGING,
    OTHER
  }
}
