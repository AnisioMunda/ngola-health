package ao.hospitalao.modules.financial.agt;

import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.entity.InvoiceItem;
import ao.hospitalao.modules.hospitals.entity.Hospital;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Gera o PDF do documento fiscal conforme modelo oficial AGT Angola.
 * Layout baseado nos modelos FACTURA.png e RECIBO.png fornecidos.
 *
 * Campos obrigatórios AGT:
 * - Cabeçalho: logo AGT, tipo documento, número, data
 * - Emitente: nome, NIF, morada, contacto
 * - Adquirente: nome, NIF
 * - Linhas: Tipo, Código, Descrição, Qt, Preço Unit, Desconto, Valor, IEC, IVA, Iselo, Total
 * - Totais: sem impostos, impostos, descontos, valor total
 * - QR Code obrigatório (canto inferior direito)
 * - Rodapé: assinatura AGT + texto de validação
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgtInvoicePdfService {

    private static final DeviceRgb AGT_BLUE  = new DeviceRgb(0, 71, 131);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(240, 240, 240);
    private static final DeviceRgb MID_GRAY   = new DeviceRgb(200, 200, 200);
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy - HH'h'mm");
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generate(Invoice invoice) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(36, 36, 36, 36);

        PdfFont bold    = PdfFontFactory.createFont(
            com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(
            com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        boolean isRecibo = invoice.getDocumentType() == Invoice.DocumentType.RC;

        // ------------------------------------------------
        // 1. Cabeçalho
        // ------------------------------------------------
        addHeader(doc, invoice, bold, regular, isRecibo);

        // ------------------------------------------------
        // 2. Dados do emitente e adquirente
        // ------------------------------------------------
        addPartiesSection(doc, invoice, bold, regular, isRecibo);

        // ------------------------------------------------
        // 3. Número e data do documento
        // ------------------------------------------------
        addDocumentInfo(doc, invoice, bold, regular);

        // ------------------------------------------------
        // 4. Tabela de linhas
        // ------------------------------------------------
        if (isRecibo) {
            addReciboTable(doc, invoice, bold, regular);
        } else {
            addFacturaTable(doc, invoice, bold, regular);
        }

        // ------------------------------------------------
        // 5. Totais retidos e totais do documento
        // ------------------------------------------------
        addTotals(doc, invoice, bold, regular);

        // ------------------------------------------------
        // 6. QR Code (obrigatório AGT)
        // ------------------------------------------------
        addQrCode(doc, invoice);

        // ------------------------------------------------
        // 7. Rodapé — validação AGT
        // ------------------------------------------------
        addFooter(doc, invoice, regular, bold);

        doc.close();
        return baos.toByteArray();
    }

    // ------------------------------------------------
    // Cabeçalho
    // ------------------------------------------------

    private void addHeader(Document doc, Invoice invoice, PdfFont bold,
                           PdfFont regular, boolean isRecibo) {
        String docTypeLabel = isRecibo ? "RECIBO"
            : switch (invoice.getDocumentType()) {
                case FT -> "FACTURA";
                case FR -> "FACTURA/RECIBO";
                case NC -> "NOTA DE CRÉDITO";
                case ND -> "NOTA DE DÉBITO";
                default -> "DOCUMENTO";
            };

        Table header = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
            .useAllAvailableWidth();

        // Esquerda: logo AGT placeholder + texto
        Cell left = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        left.add(new Paragraph("AGT")
            .setFont(bold).setFontSize(18).setFontColor(AGT_BLUE));
        left.add(new Paragraph("ADMINISTRAÇÃO\nGERAL\nTRIBUTÁRIA")
            .setFont(regular).setFontSize(8).setFontColor(AGT_BLUE));
        header.addCell(left);

        // Direita: tipo de documento
        Cell right = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        right.add(new Paragraph(docTypeLabel)
            .setFont(bold).setFontSize(26).setTextAlignment(TextAlignment.RIGHT));
        header.addCell(right);

        doc.add(header);

        // Linha separadora
        if (isRecibo) {
            doc.add(new Paragraph("Data de emissão: " +
                (invoice.getIssuedAt() != null ? invoice.getIssuedAt().format(DT_FMT) : "—"))
                .setFont(regular).setFontSize(10).setMarginTop(4));
        }
    }

    // ------------------------------------------------
    // Dados das partes (emitente / adquirente)
    // ------------------------------------------------

    private void addPartiesSection(Document doc, Invoice invoice,
                                   PdfFont bold, PdfFont regular, boolean isRecibo) {
        Hospital hosp = invoice.getHospital();

        Table parties = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
            .useAllAvailableWidth().setMarginTop(10);

        // Emitente (hospital)
        Cell emitente = new Cell().setBorder(
            new com.itextpdf.layout.borders.SolidBorder(MID_GRAY, 0.5f)).setPadding(8);
        emitente.add(new Paragraph("Contribuinte: " + (hosp != null ? hosp.getName() : ""))
            .setFont(bold).setFontSize(9));
        emitente.add(new Paragraph("Localização: " + (hosp != null && hosp.getAddress() != null
            ? hosp.getAddress() : "") + "\nLuanda/Angola")
            .setFont(regular).setFontSize(8));
        emitente.add(new Paragraph("Contacto: " + (hosp != null && hosp.getPhone() != null
            ? hosp.getPhone() : ""))
            .setFont(regular).setFontSize(8));
        emitente.add(new Paragraph("NIF: " + (hosp != null && hosp.getTaxId() != null
            ? hosp.getTaxId() : ""))
            .setFont(bold).setFontSize(8));
        parties.addCell(emitente);

        // Adquirente (paciente) — lado direito
        Cell adquirente = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setPadding(8).setTextAlignment(TextAlignment.RIGHT);
        adquirente.add(new Paragraph("Administração Geral Tributária")
            .setFont(regular).setFontSize(8));
        if (invoice.getPatientNif() != null) {
            adquirente.add(new Paragraph("Nº de Contribuinte: " + invoice.getPatientNif())
                .setFont(regular).setFontSize(8));
        }
        parties.addCell(adquirente);

        doc.add(parties);
    }

    // ------------------------------------------------
    // Informação do documento
    // ------------------------------------------------

    private void addDocumentInfo(Document doc, Invoice invoice,
                                 PdfFont bold, PdfFont regular) {
        doc.add(new Paragraph()
            .add(new Text(invoice.getDocumentType().name() + " nº " + invoice.getInvoiceNumber())
                .setFont(bold).setFontSize(11))
            .setMarginTop(12));

        String dateStr = invoice.getIssuedAt() != null
            ? invoice.getIssuedAt().format(DT_FMT) : "—";
        doc.add(new Paragraph("Data de emissão: " + dateStr)
            .setFont(regular).setFontSize(9).setMarginBottom(8));
    }

    // ------------------------------------------------
    // Tabela de linhas — Factura
    // Colunas: Tipo | Código | Descrição | Qt | Preço Unit | Desconto | Valor | IEC | IVA | Iselo | Total
    // ------------------------------------------------

    private void addFacturaTable(Document doc, Invoice invoice,
                                 PdfFont bold, PdfFont regular) {
        float[] cols = {1.2f, 1.5f, 4f, 0.8f, 1.5f, 1.2f, 1.5f, 0.8f, 0.8f, 0.8f, 1.5f};
        Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();

        // Cabeçalho
        String[] headers = {"Tipo", "Código", "Descrição", "Qt",
            "Preço Unit", "Desconto", "Valor", "IEC", "IVA", "Iselo", "Total"};
        for (String h : headers) {
            table.addHeaderCell(headerCell(h, bold));
        }

        // Linhas
        for (InvoiceItem item : invoice.getItems()) {
            table.addCell(bodyCell("S", regular));
            table.addCell(bodyCell(
                item.getServicePrice() != null ? item.getServicePrice().getCode() : "", regular));
            table.addCell(bodyCell(item.getDescription(), regular));
            table.addCell(bodyCell(String.valueOf(item.getQuantity()), regular));
            table.addCell(bodyCell(formatKz(item.getUnitPrice()), regular));
            table.addCell(bodyCell(formatPct(item.getDiscountPercent()), regular));
            // Valor = preço × quantidade sem impostos
            BigDecimal valor = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
            table.addCell(bodyCell(formatKz(valor), regular));
            table.addCell(bodyCell("—", regular)); // IEC — imposto especial
            table.addCell(bodyCell(formatKz(
                item.getLineTotal().subtract(valor)), regular)); // IVA
            table.addCell(bodyCell("—", regular)); // Iselo — imposto de selo
            table.addCell(bodyCell(formatKz(item.getLineTotal()), regular));
        }

        // Linhas vazias para preencher (mínimo 8 linhas como no modelo)
        int emptyRows = Math.max(0, 8 - invoice.getItems().size());
        for (int i = 0; i < emptyRows; i++) {
            for (int j = 0; j < 11; j++) {
                table.addCell(new Cell().setHeight(16)
                    .setBorder(new com.itextpdf.layout.borders.SolidBorder(LIGHT_GRAY, 0.3f)));
            }
        }

        doc.add(table);
    }

    // ------------------------------------------------
    // Tabela de linhas — Recibo
    // Colunas: Nº Factura | Tipo | Total s/ imp | IEC | IVA | IS | Descontos | Total
    // ------------------------------------------------

    private void addReciboTable(Document doc, Invoice invoice,
                                PdfFont bold, PdfFont regular) {
        float[] cols = {2f, 1.5f, 2f, 1f, 1f, 1f, 1.5f, 1.5f};
        Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();

        String[] headers = {"Nº Factura ou documento relevante",
            "Tipo de documento", "Total sem imposto e desconto",
            "IEC", "IVA", "IS", "Valor de descontos", "Total"};
        for (String h : headers) {
            table.addHeaderCell(headerCell(h, bold));
        }

        // Linha com a factura referenciada
        table.addCell(bodyCell(invoice.getInvoiceNumber(), regular));
        table.addCell(bodyCell("FR", regular));
        table.addCell(bodyCell(formatKz(invoice.getSubtotal()), regular));
        table.addCell(bodyCell("—", regular));
        table.addCell(bodyCell(formatKz(invoice.getVatAmount()), regular));
        table.addCell(bodyCell("—", regular));
        table.addCell(bodyCell(formatKz(invoice.getDiscountAmount()), regular));
        table.addCell(bodyCell(formatKz(invoice.getTotalAmount()), regular));

        doc.add(table);
    }

    // ------------------------------------------------
    // Totais do documento (duas colunas como no modelo)
    // ------------------------------------------------

    private void addTotals(Document doc, Invoice invoice, PdfFont bold, PdfFont regular) {
        Table totals = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
            .useAllAvailableWidth().setMarginTop(10);

        // Coluna esquerda: totais retidos na fonte (informativo)
        Cell left = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        left.add(new Paragraph("Totais retidos na fonte ou cativados pelo adquirente")
            .setFont(bold).setFontSize(8).setFontColor(ColorConstants.GRAY));
        left.add(new Paragraph("(valores informativos não integrados no total do documento)")
            .setFont(regular).setFontSize(7).setFontColor(ColorConstants.GRAY));
        totals.addCell(left);

        // Coluna direita: totais do documento
        Cell right = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        right.add(new Paragraph("Totais do documento (valores em kwanzas)")
            .setFont(bold).setFontSize(8).setTextAlignment(TextAlignment.RIGHT));

        Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{2, 1}))
            .useAllAvailableWidth().setMarginTop(4);

        addTotalRow(totalsTable, "Totais sem impostos",
            formatKz(invoice.getSubtotal()), regular);
        addTotalRow(totalsTable, "Valor de impostos",
            formatKz(invoice.getVatAmount()), regular);
        addTotalRow(totalsTable, "Valor de descontos",
            formatKz(invoice.getDiscountAmount()), regular);

        Cell labelTotal = new Cell()
            .add(new Paragraph("Valor total a pagar").setFont(bold).setFontSize(9))
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(MID_GRAY, 0.5f));
        Cell valueTotal = new Cell()
            .add(new Paragraph(formatKz(invoice.getTotalAmount()))
                .setFont(bold).setFontSize(9).setTextAlignment(TextAlignment.RIGHT))
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(MID_GRAY, 0.5f));
        totalsTable.addCell(labelTotal);
        totalsTable.addCell(valueTotal);

        right.add(totalsTable);
        totals.addCell(right);

        doc.add(totals);
    }

    // ------------------------------------------------
    // QR Code (obrigatório AGT)
    // ------------------------------------------------

    private void addQrCode(Document doc, Invoice invoice) {
        String qrData = invoice.getAgtQrCode() != null
            ? invoice.getAgtQrCode()
            : buildQrData(invoice);

        try {
            QRCodeWriter writer = new QRCodeWriter();
            var matrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 120, 120);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix);

            ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", qrBaos);

            com.itextpdf.io.image.ImageData imageData =
                com.itextpdf.io.image.ImageDataFactory.create(qrBaos.toByteArray());
            Image qr = new Image(imageData).setWidth(80).setHeight(80);

            Table qrTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .useAllAvailableWidth().setMarginTop(10);
            qrTable.addCell(new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            qrTable.addCell(new Cell().add(qr)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT));
            doc.add(qrTable);

        } catch (Exception e) {
            log.warn("Failed to generate QR Code for invoice {}: {}",
                invoice.getInvoiceNumber(), e.getMessage());
        }
    }

    // ------------------------------------------------
    // Rodapé AGT
    // ------------------------------------------------

    private void addFooter(Document doc, Invoice invoice, PdfFont regular, PdfFont bold) {
        String validationText = invoice.isAgtAccepted()
            ? "Documento validado pela AGT — Código: " + invoice.getAgtValidationCode()
            : "(valores informativos não integrados no total do documento)".repeat(5);

        doc.add(new Paragraph(validationText)
            .setFont(regular).setFontSize(7).setFontColor(ColorConstants.GRAY)
            .setMarginTop(8));

        if (invoice.isAgtAccepted()) {
            doc.add(new Paragraph("DOCUMENTO EMITIDO PELO PORTAL DO CONTRIBUINTE")
                .setFont(bold).setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.BLACK, 1))
                .setPadding(6).setMarginTop(8));
        }
    }

    // ------------------------------------------------
    // Helpers
    // ------------------------------------------------

    private Cell headerCell(String text, PdfFont bold) {
        return new Cell()
            .add(new Paragraph(text).setFont(bold).setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(LIGHT_GRAY)
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(MID_GRAY, 0.5f))
            .setPadding(3);
    }

    private Cell bodyCell(String text, PdfFont regular) {
        return new Cell()
            .add(new Paragraph(text != null ? text : "").setFont(regular).setFontSize(7))
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(LIGHT_GRAY, 0.3f))
            .setPadding(3);
    }

    private void addTotalRow(Table table, String label, String value, PdfFont regular) {
        table.addCell(new Cell()
            .add(new Paragraph(label).setFont(regular).setFontSize(8))
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(LIGHT_GRAY, 0.3f)));
        table.addCell(new Cell()
            .add(new Paragraph(value).setFont(regular).setFontSize(8)
                .setTextAlignment(TextAlignment.RIGHT))
            .setBorder(new com.itextpdf.layout.borders.SolidBorder(LIGHT_GRAY, 0.3f)));
    }

    private String formatKz(BigDecimal value) {
        if (value == null) return "0,00 Kz";
        return String.format("%,.2f Kz", value).replace(",", "X")
            .replace(".", ",").replace("X", ".");
    }

    private String formatPct(BigDecimal pct) {
        if (pct == null || pct.compareTo(BigDecimal.ZERO) == 0) return "—";
        return pct.stripTrailingZeros().toPlainString() + "%";
    }

    private String buildQrData(Invoice invoice) {
        // Formato mínimo AGT para QR Code
        return String.format("NIF:%s|DOC:%s|TOTAL:%s|DATA:%s",
            invoice.getHospital() != null ? invoice.getHospital().getTaxId() : "",
            invoice.getInvoiceNumber(),
            invoice.getTotalAmount(),
            invoice.getIssuedAt() != null ? invoice.getIssuedAt().format(DATE_FMT) : "");
    }
}