package ao.hospitalao.modules.financial.dto;

import ao.hospitalao.modules.financial.entity.Invoice.DocumentType;
import ao.hospitalao.modules.financial.entity.Invoice.InvoiceStatus;
import ao.hospitalao.modules.financial.entity.Invoice.PaymentMethod;
import ao.hospitalao.modules.financial.entity.ServicePrice.ServiceCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

public class FinancialDtos {

  // ============================================================
  // Tabela de preços de serviços
  // ============================================================

  @Data
  @Builder
  public static class ServicePriceResponse {
    private UUID id;
    private String code;
    private String description;
    private ServiceCategory category;
    private BigDecimal unitPrice;
    private BigDecimal vatRate;
    private boolean active;
  }

  @Data
  public static class CreateServicePriceRequest {
    @NotBlank
    @Size(max = 30)
    private String code;

    @NotBlank
    @Size(max = 300)
    private String description;

    @NotNull private ServiceCategory category;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal unitPrice;

    /** Taxa IVA em % (0 para serviços de saúde isentos; 14 para taxa geral) */
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal vatRate;
  }

  // ============================================================
  // Documento fiscal AGT (FT / FR / NC / ND / RC)
  // ============================================================

  @Data
  @Builder
  public static class InvoiceResponse {
    private UUID id;

    /** Número AGT: ex. "FR 2026/0000001" */
    private String invoiceNumber;

    /** Código ISO 4217 da moeda dos valores da factura */
    private String currency;

    /** Tipo: FT | FR | NC | ND | RC */
    private DocumentType documentType;

    // Partes
    private UUID patientId;
    private String patientName;

    /** NIF do paciente — obrigatório para docs > 50.000 Kz */
    private String patientNif;

    private UUID episodeId;

    // Estado do documento
    private InvoiceStatus status;
    private PaymentMethod paymentMethod;

    // Totais em AOA (Kwanzas)
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balance;

    // Seguro
    private String insuranceProvider;
    private String insurancePolicyNumber;
    private BigDecimal insuranceCoveragePercent;

    // Datas
    private OffsetDateTime issuedAt;
    private LocalDate dueDate;
    private OffsetDateTime paidAt;
    private String notes;
    private OffsetDateTime createdAt;

    // Estado AGT
    /** NAO_SUBMETIDO | PENDING | ACEITE | REJEITADO | ERRO_SUBMISSAO */
    private String agtStatus;

    /** Código de validação da AGT (após ACEITE) */
    private String agtValidationCode;

    // Linhas e pagamentos
    private List<InvoiceItemResponse> items;
    private List<PaymentResponse> payments;
  }

  @Data
  @Builder
  public static class InvoiceItemResponse {
    private UUID id;
    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;

    /** Taxa IVA em % */
    private BigDecimal vatRate;

    private BigDecimal lineTotal;
  }

  @Data
  public static class CreateInvoiceRequest {
    @NotNull private UUID patientId;

    private UUID episodeId;

    /** FT = Factura | FR = Factura/Recibo | NC = Nota de Crédito | ND = Nota de Débito */
    private DocumentType documentType;

    @Size(max = 14)
    private String patientNif;

    @Size(max = 200)
    private String patientFiscalName;

    @Size(max = 100)
    private String insuranceProvider;

    @Size(max = 50)
    private String insurancePolicyNumber;

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal insuranceCoveragePercent;

    private LocalDate dueDate;

    @Size(max = 1000)
    private String notes;

    @NotEmpty @Valid private List<@Valid CreateInvoiceItemRequest> items;
  }

  @Data
  public static class CreateInvoiceItemRequest {
    /** Se fornecido, copia descrição/preço/IVA da tabela de preços */
    private UUID servicePriceId;

    @Size(max = 300)
    private String description;

    @Min(1)
    private Integer quantity;

    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal unitPrice;

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal discountPercent;

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal vatRate;
  }

  // ============================================================
  // Pagamentos
  // ============================================================

  @Data
  @Builder
  public static class PaymentResponse {
    private UUID id;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;

    /** Referência bancária, nº cheque, código Multicaixa Express, etc. */
    private String reference;

    private OffsetDateTime paidAt;
    private String receivedByName;
    private String notes;
  }

  @Data
  public static class RegisterPaymentRequest {
    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    @NotNull private PaymentMethod paymentMethod;

    @Size(max = 100)
    private String reference;

    @Size(max = 500)
    private String notes;
  }
}
