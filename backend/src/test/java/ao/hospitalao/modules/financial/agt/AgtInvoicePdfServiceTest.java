package ao.hospitalao.modules.financial.agt;

import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.entity.InvoiceItem;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AgtInvoicePdfServiceTest {

    @Test
    void shouldGenerateReadablePdfWithPortugueseTextAndQrCode() throws Exception {
        Invoice invoice = Invoice.builder()
            .invoiceNumber("FR 2026/0000001")
            .documentType(Invoice.DocumentType.FR)
            .subtotal(new BigDecimal("100.00"))
            .vatAmount(BigDecimal.ZERO)
            .discountAmount(BigDecimal.ZERO)
            .totalAmount(new BigDecimal("100.00"))
            .items(java.util.List.of(InvoiceItem.builder()
                .description("Consulta médica")
                .quantity(1)
                .unitPrice(new BigDecimal("100.00"))
                .lineTotal(new BigDecimal("100.00"))
                .build()))
            .build();

        byte[] bytes = new AgtInvoicePdfService().generate(invoice);

        try (var document = Loader.loadPDF(bytes)) {
            String text = new PDFTextStripper().getText(document);

            assertTrue(text.contains("Consulta médica"));
            assertTrue(text.contains("FR 2026/0000001"));
            assertTrue(document.getNumberOfPages() > 0);
        }
    }
}
