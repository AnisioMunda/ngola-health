package ao.hospitalao.modules.financial.service;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.financial.agt.AgtApiClient;
import ao.hospitalao.modules.financial.agt.AgtApiClient.AgtSubmissionResult;
import ao.hospitalao.modules.financial.dto.FinancialDtos.*;
import ao.hospitalao.modules.financial.entity.*;
import ao.hospitalao.modules.financial.entity.Invoice.*;
import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import ao.hospitalao.modules.financial.repository.ServicePriceRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialService {

    private final InvoiceRepository      invoiceRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final PatientRepository      patientRepository;
    private final EpisodeRepository      episodeRepository;
    private final UserRepository         userRepository;
    private final HospitalRepository     hospitalRepository;
    private final AgtApiClient           agtApiClient;

    // ------------------------------------------------
    // Tabela de preços
    // ------------------------------------------------

    @Transactional(readOnly = true)
    public List<ServicePriceResponse> findAllPrices() {
        UUID hospitalId = TenantContext.getCurrentHospital();
        return servicePriceRepository.findByHospitalIdAndActiveTrue(hospitalId)
            .stream().map(this::toPriceResponse).collect(Collectors.toList());
    }

    @SuppressWarnings("null")
    @Transactional
    public ServicePriceResponse createPrice(CreateServicePriceRequest req) {
        UUID hospitalId = TenantContext.getCurrentHospital();
        if (servicePriceRepository.existsByHospitalIdAndCode(hospitalId, req.getCode())) {
            throw new IllegalArgumentException("Código já existe: " + req.getCode());
        }
        ServicePrice price = ServicePrice.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .code(req.getCode().toUpperCase())
            .description(req.getDescription())
            .category(req.getCategory())
            .unitPrice(req.getUnitPrice())
            .vatRate(req.getVatRate() != null ? req.getVatRate() : BigDecimal.ZERO)
            .build();
        return toPriceResponse(servicePriceRepository.save(price));
    }

    // ------------------------------------------------
    // Documentos fiscais
    // ------------------------------------------------

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> findAll(UUID patientId, InvoiceStatus status, Pageable pageable) {
        UUID hospitalId = TenantContext.getCurrentHospital();
        return invoiceRepository.findWithFilters(hospitalId, patientId, status, pageable)
            .map(this::toInvoiceResponse);
    }

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public InvoiceResponse findById(UUID id) {
        return invoiceRepository.findById(id)
            .map(this::toInvoiceResponse)
            .orElseThrow(() -> new EntityNotFoundException("Documento não encontrado: " + id));
    }

    @SuppressWarnings("null")
    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest req) {
        UUID hospitalId = TenantContext.getCurrentHospital();

        var patient = patientRepository.findById(req.getPatientId())
            .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado"));

        DocumentType docType = req.getDocumentType() != null
            ? req.getDocumentType() : DocumentType.FR;

        // Número AGT: ex. "FR 2026/0000001"
        Long seq = invoiceRepository.nextSequence(docType.name());
        String invoiceNumber = String.format("%s %d/%07d",
            docType.name(), Year.now().getValue(), seq);

        Invoice invoice = Invoice.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .invoiceNumber(invoiceNumber)
            .documentType(docType)
            .patient(patient)
            .patientNif(req.getPatientNif())
            .patientFiscalName(req.getPatientFiscalName() != null
                ? req.getPatientFiscalName() : patient.getFullName())
            .status(InvoiceStatus.RASCUNHO)
            .insuranceProvider(req.getInsuranceProvider())
            .insurancePolicyNumber(req.getInsurancePolicyNumber())
            .insuranceCoveragePercent(req.getInsuranceCoveragePercent() != null
                ? req.getInsuranceCoveragePercent() : BigDecimal.ZERO)
            .dueDate(req.getDueDate())
            .notes(req.getNotes())
            .agtStatus("NAO_SUBMETIDO")
            .createdBy(getCurrentUser())
            .build();

        if (req.getEpisodeId() != null) {
            invoice.setEpisode(episodeRepository.getReferenceById(req.getEpisodeId()));
        }

        // Linhas
        if (req.getItems() != null) {
            for (CreateInvoiceItemRequest itemReq : req.getItems()) {
                ServicePrice sp = null;
                BigDecimal unitPrice = itemReq.getUnitPrice();
                BigDecimal vatRate   = itemReq.getVatRate() != null
                    ? itemReq.getVatRate() : BigDecimal.ZERO;
                String description   = itemReq.getDescription();

                if (itemReq.getServicePriceId() != null) {
                    sp = servicePriceRepository.findById(itemReq.getServicePriceId())
                        .orElseThrow(() -> new EntityNotFoundException("Preço não encontrado"));
                    if (unitPrice == null)  unitPrice   = sp.getUnitPrice();
                    if (vatRate.compareTo(BigDecimal.ZERO) == 0) vatRate = sp.getVatRate();
                    if (description == null) description = sp.getDescription();
                }

                InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice).servicePrice(sp)
                    .description(description)
                    .quantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 1)
                    .unitPrice(unitPrice)
                    .discountPercent(itemReq.getDiscountPercent() != null
                        ? itemReq.getDiscountPercent() : BigDecimal.ZERO)
                    .vatRate(vatRate).lineTotal(BigDecimal.ZERO)
                    .build();
                item.calculateTotal();
                invoice.getItems().add(item);
            }
        }

        recalculateTotals(invoice);
        Invoice saved = invoiceRepository.save(invoice);
        log.info("Documento criado: {} para paciente {}", saved.getInvoiceNumber(), patient.getFullName());
        return toInvoiceResponse(saved);
    }

    /**
     * Emitir documento e submeter à AGT.
     * RASCUNHO → EMITIDO + submissão assíncrona à AGT.
     */
    @Transactional
    public InvoiceResponse issue(UUID id) {
        Invoice invoice = getOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.RASCUNHO) {
            throw new IllegalStateException("Apenas documentos em RASCUNHO podem ser emitidos.");
        }

        invoice.setStatus(InvoiceStatus.EMITIDO);
        invoice.setIssuedAt(OffsetDateTime.now());
        Invoice saved = invoiceRepository.save(invoice);

        // Submeter à AGT de forma assíncrona
        submitToAgt(saved);

        log.info("Documento emitido: {}", saved.getInvoiceNumber());
        return toInvoiceResponse(saved);
    }

    @Transactional
    public InvoiceResponse registerPayment(UUID id, RegisterPaymentRequest req) {
        Invoice invoice = getOrThrow(id);

        if (invoice.getStatus() == InvoiceStatus.PAGO
            || invoice.getStatus() == InvoiceStatus.ANULADO) {
            throw new IllegalStateException(
                "Não é possível registar pagamento para documento " + invoice.getStatus());
        }

        BigDecimal balance = invoice.getBalance();
        if (req.getAmount().compareTo(balance) > 0) {
            throw new IllegalArgumentException(
                "Valor excede o saldo em dívida. Saldo: " + balance + " AOA");
        }

        Payment payment = Payment.builder()
            .invoice(invoice).amount(req.getAmount())
            .paymentMethod(req.getPaymentMethod())
            .reference(req.getReference()).notes(req.getNotes())
            .receivedBy(getCurrentUser())
            .build();

        invoice.getPayments().add(payment);
        invoice.setPaidAmount(invoice.getPaidAmount().add(req.getAmount()));
        invoice.setPaymentMethod(req.getPaymentMethod());

        if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAGO);
            invoice.setPaidAt(OffsetDateTime.now());
        } else {
            invoice.setStatus(InvoiceStatus.PAGO_PARCIALMENTE);
        }

        log.info("Pagamento de {} AOA registado para {}", req.getAmount(), invoice.getInvoiceNumber());
        return toInvoiceResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse void_(UUID id, String reason) {
        Invoice invoice = getOrThrow(id);
        if (invoice.getStatus() == InvoiceStatus.PAGO) {
            throw new IllegalStateException(
                "Não é possível anular documento pago. Emita uma Nota de Crédito.");
        }
        invoice.setStatus(InvoiceStatus.ANULADO);
        invoice.setNotes((invoice.getNotes() != null ? invoice.getNotes() + "\n" : "")
            + "ANULADO: " + reason);
        log.info("Documento anulado: {}", invoice.getInvoiceNumber());
        return toInvoiceResponse(invoiceRepository.save(invoice));
    }

    // ------------------------------------------------
    // Submissão à AGT (após emissão)
    // ------------------------------------------------

    private void submitToAgt(Invoice invoice) {
        try {
            AgtSubmissionResult result = agtApiClient.register(invoice);
            if (result.requestId() != null) {
                invoice.setAgtRequestId(result.requestId());
                invoice.setAgtStatus("PENDING");
                invoice.setAgtSubmittedAt(OffsetDateTime.now());
            } else {
                invoice.setAgtStatus("ERRO_SUBMISSAO");
                invoice.setAgtErrorMessage(result.errorMessage());
            }
            invoiceRepository.save(invoice);
        } catch (Exception e) {
            log.error("Erro ao submeter factura {} à AGT: {}",
                invoice.getInvoiceNumber(), e.getMessage());
            invoice.setAgtStatus("ERRO_SUBMISSAO");
            invoice.setAgtErrorMessage(e.getMessage());
            invoiceRepository.save(invoice);
        }
    }

    // ------------------------------------------------
    // Helpers
    // ------------------------------------------------

    private void recalculateTotals(Invoice invoice) {
        BigDecimal subtotal  = BigDecimal.ZERO;
        BigDecimal vatAmount = BigDecimal.ZERO;
        for (InvoiceItem item : invoice.getItems()) {
            BigDecimal base = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal disc = base.multiply(item.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal afterDisc = base.subtract(disc);
            BigDecimal vat = afterDisc.multiply(item.getVatRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            subtotal  = subtotal.add(afterDisc);
            vatAmount = vatAmount.add(vat);
        }
        invoice.setSubtotal(subtotal);
        invoice.setVatAmount(vatAmount);
        invoice.setTotalAmount(subtotal.add(vatAmount).subtract(invoice.getDiscountAmount()));
    }

    @SuppressWarnings("null")
    private Invoice getOrThrow(UUID id) {
        return invoiceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Documento não encontrado: " + id));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private ServicePriceResponse toPriceResponse(ServicePrice p) {
        return ServicePriceResponse.builder()
            .id(p.getId()).code(p.getCode()).description(p.getDescription())
            .category(p.getCategory()).unitPrice(p.getUnitPrice())
            .vatRate(p.getVatRate()).active(p.isActive()).build();
    }

    private InvoiceResponse toInvoiceResponse(Invoice inv) {
        List<InvoiceItemResponse> items = inv.getItems().stream()
            .map(i -> InvoiceItemResponse.builder()
                .id(i.getId()).description(i.getDescription())
                .quantity(i.getQuantity()).unitPrice(i.getUnitPrice())
                .discountPercent(i.getDiscountPercent()).vatRate(i.getVatRate())
                .lineTotal(i.getLineTotal()).build())
            .collect(Collectors.toList());

        List<PaymentResponse> payments = inv.getPayments().stream()
            .map(p -> PaymentResponse.builder()
                .id(p.getId()).amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod()).reference(p.getReference())
                .paidAt(p.getPaidAt())
                .receivedByName(p.getReceivedBy() != null ? p.getReceivedBy().getFullName() : null)
                .notes(p.getNotes()).build())
            .collect(Collectors.toList());

        return InvoiceResponse.builder()
            .id(inv.getId()).invoiceNumber(inv.getInvoiceNumber())
            .documentType(inv.getDocumentType())
            .patientId(inv.getPatient().getId()).patientName(inv.getPatient().getFullName())
            .patientNif(inv.getPatientNif())
            .episodeId(inv.getEpisode() != null ? inv.getEpisode().getId() : null)
            .status(inv.getStatus()).paymentMethod(inv.getPaymentMethod())
            .subtotal(inv.getSubtotal()).discountAmount(inv.getDiscountAmount())
            .vatAmount(inv.getVatAmount()).totalAmount(inv.getTotalAmount())
            .paidAmount(inv.getPaidAmount()).balance(inv.getBalance())
            .insuranceProvider(inv.getInsuranceProvider())
            .insurancePolicyNumber(inv.getInsurancePolicyNumber())
            .insuranceCoveragePercent(inv.getInsuranceCoveragePercent())
            .issuedAt(inv.getIssuedAt()).dueDate(inv.getDueDate()).paidAt(inv.getPaidAt())
            .notes(inv.getNotes()).createdAt(inv.getCreatedAt())
            .agtStatus(inv.getAgtStatus())
            .agtValidationCode(inv.getAgtValidationCode())
            .items(items).payments(payments)
            .build();
    }
}