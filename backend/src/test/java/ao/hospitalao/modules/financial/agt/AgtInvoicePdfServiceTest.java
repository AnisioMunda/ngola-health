package ao.hospitalao.modules.financial.agt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.entity.InvoiceItem;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.math.BigDecimal;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class AgtInvoicePdfServiceTest {

  @Test
  void shouldGenerateReadablePdfWithPortugueseTextAndQrCode() throws Exception {
    Invoice invoice =
        Invoice.builder()
            .invoiceNumber("FR 2026/0000001")
            .documentType(Invoice.DocumentType.FR)
            .subtotal(new BigDecimal("100.00"))
            .vatAmount(BigDecimal.ZERO)
            .discountAmount(BigDecimal.ZERO)
            .totalAmount(new BigDecimal("100.00"))
            .items(
                java.util.List.of(
                    InvoiceItem.builder()
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

      var page = new PDFRenderer(document).renderImageWithDPI(0, 200);
      var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(page)));
      String qrData = new MultiFormatReader().decode(bitmap).getText();

      assertEquals("NIF:|DOC:FR 2026/0000001|TOTAL:100.00|DATA:", qrData);
    }
  }
}
