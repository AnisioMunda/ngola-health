package ao.hospitalao.modules.financial.dto;

import ao.hospitalao.modules.financial.entity.Invoice.DocumentType;
import ao.hospitalao.modules.financial.entity.Invoice.InvoiceStatus;
import ao.hospitalao.modules.financial.entity.Invoice.PaymentMethod;
import ao.hospitalao.modules.financial.entity.ServicePrice.ServiceCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class FinancialDtos {

    // ============================================================
    // Tabela de preços de serviços
    // ============================================================

    @Data @Builder
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
        private String code;
        private String description;
        private ServiceCategory category;
        private BigDecimal unitPrice;
        /** Taxa IVA em % (0 para serviços de saúde isentos; 14 para taxa geral) */
        private BigDecimal vatRate;
    }

    // ============================================================
    // Documento fiscal AGT (FT / FR / NC / ND / RC)
    // ============================================================

    @Data @Builder
    public static class InvoiceResponse {
        private UUID   id;
        /** Número AGT: ex. "FR 2026/0000001" */
        private String invoiceNumber;
        /** Tipo: FT | FR | NC | ND | RC */
        private DocumentType documentType;

        // Partes
        private UUID   patientId;
        private String patientName;
        /** NIF do paciente — obrigatório para docs > 50.000 Kz */
        private String patientNif;
        private UUID   episodeId;

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
        private String     insuranceProvider;
        private String     insurancePolicyNumber;
        private BigDecimal insuranceCoveragePercent;

        // Datas
        private OffsetDateTime issuedAt;
        private LocalDate      dueDate;
        private OffsetDateTime paidAt;
        private String         notes;
        private OffsetDateTime createdAt;

        // Estado AGT
        /** NAO_SUBMETIDO | PENDING | ACEITE | REJEITADO | ERRO_SUBMISSAO */
        private String agtStatus;
        /** Código de validação da AGT (após ACEITE) */
        private String agtValidationCode;

        // Linhas e pagamentos
        private List<InvoiceItemResponse> items;
        private List<PaymentResponse>     payments;
    }

    @Data @Builder
    public static class InvoiceItemResponse {
        private UUID       id;
        private String     description;
        private Integer    quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountPercent;
        /** Taxa IVA em % */
        private BigDecimal vatRate;
        private BigDecimal lineTotal;
    }

    @Data
    public static class CreateInvoiceRequest {
        private UUID         patientId;
        private UUID         episodeId;
        /** FT = Factura | FR = Factura/Recibo | NC = Nota de Crédito | ND = Nota de Débito */
        private DocumentType documentType;
        private String       patientNif;
        private String       patientFiscalName;
        private String       insuranceProvider;
        private String       insurancePolicyNumber;
        private BigDecimal   insuranceCoveragePercent;
        private LocalDate    dueDate;
        private String       notes;
        private List<CreateInvoiceItemRequest> items;
    }

    @Data
    public static class CreateInvoiceItemRequest {
        /** Se fornecido, copia descrição/preço/IVA da tabela de preços */
        private UUID       servicePriceId;
        private String     description;
        private Integer    quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountPercent;
        private BigDecimal vatRate;
    }

    // ============================================================
    // Pagamentos
    // ============================================================

    @Data @Builder
    public static class PaymentResponse {
        private UUID           id;
        private BigDecimal     amount;
        private PaymentMethod  paymentMethod;
        /** Referência bancária, nº cheque, código Multicaixa Express, etc. */
        private String         reference;
        private OffsetDateTime paidAt;
        private String         receivedByName;
        private String         notes;
    }

    @Data
    public static class RegisterPaymentRequest {
        private BigDecimal    amount;
        private PaymentMethod paymentMethod;
        private String        reference;
        private String        notes;
    }
}