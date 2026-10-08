package ao.hospitalao.modules.financial.agt;

import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.entity.InvoiceItem;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.shared.pdf.PdfDocumentBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgtInvoicePdfService {

  private static final Color AGT_BLUE = new Color(0, 71, 131);
  private static final DateTimeFormatter DT_FMT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy - HH'h'mm");

  public byte[] generate(Invoice invoice) throws IOException, WriterException {
    Hospital hospital = invoice.getHospital();
    boolean isRecibo = invoice.getDocumentType() == Invoice.DocumentType.RC;
    String documentTitle = getDocumentTitle(invoice, isRecibo);

    try (PdfDocumentBuilder pdf = new PdfDocumentBuilder(36)) {
      pdf.addBanner("AGT - ADMINISTRAÇÃO GERAL TRIBUTÁRIA", documentTitle, AGT_BLUE, Color.WHITE);

      pdf.addTable(
          new String[] {"Emitente", "Adquirente"},
          rows(
              "Contribuinte: " + (hospital != null ? hospital.getName() : ""),
              "Administração Geral Tributária",
              "Localização: "
                  + (hospital != null ? nvl(hospital.getAddress()) : "")
                  + " Luanda/Angola",
              "Nº de Contribuinte: " + nvl(invoice.getPatientNif()),
              "Contacto: " + (hospital != null ? nvl(hospital.getPhone()) : ""),
              "NIF: " + (hospital != null ? nvl(hospital.getTaxId()) : "")),
          new float[] {1, 1},
          8,
          new Color(240, 240, 240),
          Color.DARK_GRAY);

      pdf.addSection("DOCUMENTO");
      pdf.addText(
          invoice.getDocumentType().name() + " nº " + invoice.getInvoiceNumber(),
          11,
          Color.DARK_GRAY,
          true,
          4);
      pdf.addText(
          "Data de emissão: "
              + (invoice.getIssuedAt() != null ? invoice.getIssuedAt().format(DT_FMT) : "-"),
          9,
          Color.DARK_GRAY,
          false,
          5);

      if (isRecibo) {
        addReceiptItems(pdf, invoice);
      } else {
        addInvoiceItems(pdf, invoice);
      }

      pdf.addSection("TOTAIS DO DOCUMENTO (KWANZAS)");
      pdf.addTable(
          new String[] {"Descrição", "Valor"},
          rows(
              "Totais sem impostos", formatKz(invoice.getSubtotal()),
              "Valor de impostos", formatKz(invoice.getVatAmount()),
              "Valor de descontos", formatKz(invoice.getDiscountAmount()),
              "Valor total a pagar", formatKz(invoice.getTotalAmount())),
          new float[] {2, 1},
          9,
          new Color(240, 240, 240),
          Color.DARK_GRAY);

      addQrCode(pdf, invoice);
      addValidation(pdf, invoice);
      return pdf.toByteArray();
    }
  }

  private void addInvoiceItems(PdfDocumentBuilder pdf, Invoice invoice) throws IOException {
    pdf.addSection("LINHAS DA FACTURA");
    List<String[]> rows = new ArrayList<>();
    for (InvoiceItem item : invoice.getItems()) {
      BigDecimal value = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
      rows.add(
          new String[] {
            "S",
            item.getServicePrice() != null ? item.getServicePrice().getCode() : "",
            item.getDescription(),
            String.valueOf(item.getQuantity()),
            formatKz(item.getUnitPrice()),
            formatPct(item.getDiscountPercent()),
            formatKz(value),
            "-",
            formatKz(item.getLineTotal().subtract(value)),
            "-",
            formatKz(item.getLineTotal())
          });
    }
    for (int i = rows.size(); i < 8; i++) {
      rows.add(new String[] {"", "", "", "", "", "", "", "", "", "", ""});
    }
    pdf.addTable(
        new String[] {
          "Tipo",
          "Código",
          "Descrição",
          "Qt",
          "Preço Unit",
          "Desconto",
          "Valor",
          "IEC",
          "IVA",
          "IS",
          "Total"
        },
        rows,
        new float[] {1.2f, 1.5f, 4, 0.8f, 1.5f, 1.2f, 1.5f, 0.8f, 0.8f, 0.8f, 1.5f},
        6,
        new Color(240, 240, 240),
        Color.DARK_GRAY);
  }

  private void addReceiptItems(PdfDocumentBuilder pdf, Invoice invoice) throws IOException {
    pdf.addSection("DOCUMENTO DE REFERÊNCIA");
    pdf.addTable(
        new String[] {
          "Nº documento", "Tipo", "Total sem impostos", "IEC", "IVA", "IS", "Descontos", "Total"
        },
        rows(
            invoice.getInvoiceNumber(),
            "FR",
            formatKz(invoice.getSubtotal()),
            "-",
            formatKz(invoice.getVatAmount()),
            "-",
            formatKz(invoice.getDiscountAmount()),
            formatKz(invoice.getTotalAmount())),
        new float[] {2, 1.5f, 2, 1, 1, 1, 1.5f, 1.5f},
        7,
        new Color(240, 240, 240),
        Color.DARK_GRAY);
  }

  private void addQrCode(PdfDocumentBuilder pdf, Invoice invoice)
      throws IOException, WriterException {
    String qrData = invoice.getAgtQrCode();
    if (!invoice.isAgtAccepted() || qrData == null || qrData.isBlank()) {
      return;
    }

    var matrix = new QRCodeWriter().encode(qrData, BarcodeFormat.QR_CODE, 120, 120);
    BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
    pdf.addImage(image, 80, 80);
  }

  private void addValidation(PdfDocumentBuilder pdf, Invoice invoice) throws IOException {
    if (invoice.isAgtAccepted()) {
      String validationCode = invoice.getAgtValidationCode();
      pdf.addText(
          validationCode == null || validationCode.isBlank()
              ? "Documento validado pela AGT."
              : "Documento validado pela AGT - Código: " + validationCode,
          8,
          Color.GRAY,
          false,
          4);
    } else {
      pdf.addText("Documento ainda não validado pela AGT.", 8, Color.GRAY, false, 4);
    }
  }

  private String getDocumentTitle(Invoice invoice, boolean isRecibo) {
    if (isRecibo) return "RECIBO";
    return switch (invoice.getDocumentType()) {
      case FT -> "FACTURA";
      case FR -> "FACTURA/RECIBO";
      case NC -> "NOTA DE CRÉDITO";
      case ND -> "NOTA DE DÉBITO";
      default -> "DOCUMENTO";
    };
  }

  private List<String[]> rows(String... values) {
    List<String[]> rows = new ArrayList<>();
    for (int i = 0; i < values.length; i += 2) {
      rows.add(new String[] {values[i], values[i + 1]});
    }
    return rows;
  }

  private String formatKz(BigDecimal value) {
    if (value == null) return "0,00 Kz";
    return String.format("%,.2f Kz", value).replace(",", "X").replace(".", ",").replace("X", ".");
  }

  private String formatPct(BigDecimal percent) {
    if (percent == null || percent.compareTo(BigDecimal.ZERO) == 0) return "-";
    return percent.stripTrailingZeros().toPlainString() + "%";
  }

  private String nvl(String value) {
    return value != null ? value : "";
  }
}
