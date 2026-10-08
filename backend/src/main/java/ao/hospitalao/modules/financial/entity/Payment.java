package ao.hospitalao.modules.financial.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @Column(name = "amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "payment_method", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private Invoice.PaymentMethod paymentMethod;

  @Column(name = "reference", length = 100)
  private String reference;

  @Column(name = "paid_at", nullable = false)
  private OffsetDateTime paidAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "received_by")
  private User receivedBy;

  @Column(name = "notes", length = 500)
  private String notes;

  @PrePersist
  protected void onCreate() {
    if (this.paidAt == null) this.paidAt = OffsetDateTime.now();
  }
}
