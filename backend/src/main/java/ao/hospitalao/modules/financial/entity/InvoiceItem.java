package ao.hospitalao.modules.financial.entity;

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
public class InvoiceItem {

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

  public void calculateTotal() {
    BigDecimal qty = BigDecimal.valueOf(quantity);
    BigDecimal base = unitPrice.multiply(qty);
    BigDecimal disc = base.multiply(discountPercent).divide(BigDecimal.valueOf(100));
    BigDecimal afterDiscount = base.subtract(disc);
    BigDecimal vat = afterDiscount.multiply(vatRate).divide(BigDecimal.valueOf(100));
    this.lineTotal = afterDiscount.add(vat);
  }
}
