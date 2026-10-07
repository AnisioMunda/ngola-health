package ao.hospitalao.modules.financial.controller;

import ao.hospitalao.modules.financial.agt.AgtInvoicePdfService;
import ao.hospitalao.modules.financial.dto.FinancialDtos.*;
import ao.hospitalao.modules.financial.entity.Invoice.InvoiceStatus;
import ao.hospitalao.modules.financial.repository.InvoiceRepository;
import ao.hospitalao.modules.financial.service.FinancialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/financial")
@RequiredArgsConstructor
@Tag(name = "Financial", description = "Facturação electrónica AGT Angola — DP 50/19")
@SecurityRequirement(name = "bearerAuth")
public class FinancialController {

  private final FinancialService financialService;
  private final AgtInvoicePdfService pdfService;
  private final InvoiceRepository invoiceRepository;

  // ------------------------------------------------
  // Tabela de preços de serviços
  // ------------------------------------------------

  @GetMapping("/prices")
  @Operation(summary = "Listar tabela de preços de serviços")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<ServicePriceResponse>> findAllPrices() {
    return ResponseEntity.ok(financialService.findAllPrices());
  }

  @PostMapping("/prices")
  @Operation(summary = "Adicionar preço de serviço")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','FINANCIAL')")
  public ResponseEntity<ServicePriceResponse> createPrice(
      @Valid @RequestBody CreateServicePriceRequest request) {
    return ResponseEntity.ok(financialService.createPrice(request));
  }

  // ------------------------------------------------
  // Documentos fiscais (FT / FR / NC / ND / RC)
  // ------------------------------------------------

  @GetMapping("/invoices")
  @Operation(summary = "Listar documentos fiscais")
  @PreAuthorize("hasAnyRole('ADMIN','FINANCIAL','MANAGER','RECEPTIONIST')")
  public ResponseEntity<Page<InvoiceResponse>> findAll(
      @RequestParam(required = false) UUID patientId,
      @RequestParam(required = false) InvoiceStatus status,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(financialService.findAll(patientId, status, pageable));
  }

  @GetMapping("/invoices/{id}")
  @Operation(summary = "Obter documento fiscal por ID")
  @PreAuthorize("hasAnyRole('ADMIN','FINANCIAL','MANAGER','RECEPTIONIST')")
  public ResponseEntity<InvoiceResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(financialService.findById(id));
  }

  @PostMapping("/invoices")
  @Operation(summary = "Criar documento fiscal (FT / FR / NC / ND)")
  @PreAuthorize("hasAnyRole('ADMIN','FINANCIAL','RECEPTIONIST')")
  public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest request) {
    InvoiceResponse created = financialService.create(request);
    URI uri =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(uri).body(created);
  }

  @PatchMapping("/invoices/{id}/issue")
  @Operation(summary = "Emitir documento fiscal (RASCUNHO → EMITIDO + submissão AGT)")
  @PreAuthorize("hasAnyRole('ADMIN','FINANCIAL','RECEPTIONIST')")
  public ResponseEntity<InvoiceResponse> issue(@PathVariable UUID id) {
    return ResponseEntity.ok(financialService.issue(id));
  }

  @PostMapping("/invoices/{id}/payments")
  @Operation(summary = "Registar pagamento (parcial ou total)")
  @PreAuthorize("hasAnyRole('ADMIN','FINANCIAL','RECEPTIONIST')")
  public ResponseEntity<InvoiceResponse> registerPayment(
      @PathVariable UUID id, @Valid @RequestBody RegisterPaymentRequest request) {
    return ResponseEntity.ok(financialService.registerPayment(id, request));
  }

  @PatchMapping("/invoices/{id}/void")
  @Operation(summary = "Anular documento fiscal")
  @PreAuthorize("hasAnyRole('ADMIN','FINANCIAL')")
  public ResponseEntity<InvoiceResponse> void_(@PathVariable UUID id, @RequestParam String reason) {
    return ResponseEntity.ok(financialService.void_(id, reason));
  }

  // ------------------------------------------------
  // PDF do documento fiscal — layout oficial AGT
  // ------------------------------------------------

  @GetMapping("/invoices/{id}/pdf")
  @Operation(summary = "Gerar PDF conforme modelo oficial AGT Angola")
  @PreAuthorize("hasAnyRole('ADMIN','FINANCIAL','MANAGER','RECEPTIONIST')")
  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) throws Exception {

    // 1ª Query: Carrega o Invoice com hospital, patient, episode e ITEMS
    var invoice =
        invoiceRepository
            .findByIdWithItems(id)
            .orElseThrow(() -> new EntityNotFoundException("Documento não encontrado: " + id));

    // 2ª Query: Carrega os PAYMENTS na mesma transação/sessão Hibernate.
    // O Hibernate anexa a lista de pagamentos à instância 'invoice' acima.
    invoiceRepository.findByIdWithPayments(id);

    byte[] pdf = pdfService.generate(invoice);

    // Nome do ficheiro: ex. "FR-2026-0000001.pdf"
    String filename = invoice.getInvoiceNumber().replace(" ", "-").replace("/", "-") + ".pdf";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
    headers.setContentLength(pdf.length);

    return ResponseEntity.ok().headers(headers).body(pdf);
  }
}
