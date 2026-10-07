package ao.hospitalao.modules.financial.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.episodes.entity.Episode;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.entity.TenantScopedEntity;
import ao.hospitalao.modules.patients.entity.Patient;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false, insertable = false, updatable = false)
  private Hospital hospital;

  /**
   * Número do documento fiscal AGT Angola. Formato: {TIPO} {ANO}/{SEQUÊNCIA 7 dígitos} Ex: FR
   * 2026/0000001 | FT 2026/0000001 | NC 2026/0000001
   */
  @Column(name = "invoice_number", nullable = false, unique = true, length = 30)
  private String invoiceNumber;

  /**
   * Tipo de documento fiscal — DP 50/19 AGT Angola: FT = Factura | FR = Factura/Recibo | NC = Nota
   * de Crédito ND = Nota de Débito | RC = Recibo
   */
  @Column(name = "document_type", nullable = false, length = 5)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private DocumentType documentType = DocumentType.FR;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "episode_id")
  private Episode episode;

  @Column(name = "status", nullable = false, length = 25)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private InvoiceStatus status = InvoiceStatus.RASCUNHO;

  @Column(name = "payment_method", length = 30)
  @Enumerated(EnumType.STRING)
  private PaymentMethod paymentMethod;

  // ------------------------------------------------
  // Identificação fiscal — obrigatório AGT
  // ------------------------------------------------

  /** NIF do paciente — obrigatório para docs > 50.000 Kz */
  @Column(name = "patient_nif", length = 14)
  private String patientNif;

  /** Nome fiscal do paciente conforme NIF */
  @Column(name = "patient_fiscal_name", length = 200)
  private String patientFiscalName;

  // ------------------------------------------------
  // Totais em Kwanzas (AOA)
  // ------------------------------------------------

  @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal subtotal = BigDecimal.ZERO;

  @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal discountAmount = BigDecimal.ZERO;

  /** IVA — Imposto sobre o Valor Acrescentado (14% geral; 0% saúde) */
  @Column(name = "vat_amount", nullable = false, precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal vatAmount = BigDecimal.ZERO;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal paidAmount = BigDecimal.ZERO;

  // ------------------------------------------------
  // Seguro de saúde (ENSA, AAA, Tranquilidade, etc.)
  // ------------------------------------------------

  @Column(name = "insurance_provider", length = 100)
  private String insuranceProvider;

  @Column(name = "insurance_policy_number", length = 50)
  private String insurancePolicyNumber;

  @Column(name = "insurance_coverage_percent", precision = 5, scale = 2)
  @Builder.Default
  private BigDecimal insuranceCoveragePercent = BigDecimal.ZERO;

  // ------------------------------------------------
  // Datas
  // ------------------------------------------------

  @Column(name = "issued_at")
  private OffsetDateTime issuedAt;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "paid_at")
  private OffsetDateTime paidAt;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  // ------------------------------------------------
  // Integração AGT — campos obrigatórios
  // ------------------------------------------------

  /** requestID devolvido pela API AGT após submissão */
  @Column(name = "agt_request_id", length = 100)
  private String agtRequestId;

  /** Estado na AGT: NAO_SUBMETIDO / PENDING / ACEITE / REJEITADO */
  @Column(name = "agt_status", length = 20)
  @Builder.Default
  private String agtStatus = "NAO_SUBMETIDO";

  /** Código de validação atribuído pela AGT (após ACEITE) */
  @Column(name = "agt_validation_code", length = 100)
  private String agtValidationCode;

  /** Dados do QR Code a imprimir no documento fiscal */
  @Column(name = "agt_qr_code", columnDefinition = "TEXT")
  private String agtQrCode;

  /** Mensagem de erro da AGT em caso de rejeição */
  @Column(name = "agt_error_message", length = 500)
  private String agtErrorMessage;

  /** Data/hora de submissão à API AGT */
  @Column(name = "agt_submitted_at")
  private OffsetDateTime agtSubmittedAt;

  // ------------------------------------------------
  // Relações
  // ------------------------------------------------

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<InvoiceItem> items = new ArrayList<>();

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Payment> payments = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

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

  public BigDecimal getBalance() {
    return totalAmount.subtract(paidAmount);
  }

  public boolean isAgtAccepted() {
    return "ACEITE".equals(agtStatus);
  }

  // ------------------------------------------------
  // Enums AGT Angola — DP 50/19
  // ------------------------------------------------

  public enum DocumentType {
    FT,
    FR,
    NC,
    ND,
    RC
  }

  public enum InvoiceStatus {
    RASCUNHO,
    EMITIDO,
    PAGO_PARCIALMENTE,
    PAGO,
    ANULADO,
    EM_ATRASO
  }

  public enum PaymentMethod {
    NUMERARIO,
    TRANSFERENCIA,
    CARTAO,
    SEGURO,
    CHEQUE,
    DINHEIRO_MOVEL
  }
}
