package ao.hospitalao.modules.financial.entity;

import ao.hospitalao.modules.financial.util.FinancialAmounts;
import ao.hospitalao.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "invoice_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_price_id")
  private ServicePrice servicePrice;

  @Column(name = "description", nullable = false, length = 300)
  private String description;

  @Column(name = "quantity", nullable = false)
  @Builder.Default
  private Integer quantity = 1;

  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "discount_percent", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal discountPercent = BigDecimal.ZERO;

  @Column(name = "vat_rate", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal vatRate = BigDecimal.ZERO;

  @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
  private BigDecimal lineTotal;

  public BigDecimal getNetAmount() {
    BigDecimal base = FinancialAmounts.round(unitPrice).multiply(BigDecimal.valueOf(quantity));
    BigDecimal discount = FinancialAmounts.percentage(base, discountPercent);
    return FinancialAmounts.round(base.subtract(discount));
  }

  public BigDecimal getVatAmount() {
    return FinancialAmounts.percentage(getNetAmount(), vatRate);
  }

  public void calculateTotal() {
    this.lineTotal = FinancialAmounts.round(getNetAmount().add(getVatAmount()));
  }
}
