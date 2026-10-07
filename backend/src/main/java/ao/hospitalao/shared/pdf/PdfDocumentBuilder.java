package ao.hospitalao.shared.pdf;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

public final class PdfDocumentBuilder implements AutoCloseable {

  private static final Color BORDER = new Color(210, 216, 220);
  private static final Color ALT_ROW = new Color(248, 249, 251);
  private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
  private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
  private static final PDType1Font FONT_REGULAR =
      new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  private static final PDType1Font FONT_BOLD =
      new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

  private final PDDocument document = new PDDocument();
  private final float margin;
  private PDPage page;
  private PDPageContentStream stream;
  private float y;
  private boolean saved;

  public PdfDocumentBuilder(float margin) throws IOException {
    this.margin = margin;
    addPage();
  }

  public void addBanner(String title, String subtitle, Color background, Color accent)
      throws IOException {
    ensureSpace(58);
    float top = y;
    stream.setNonStrokingColor(background);
    stream.addRect(margin, top - 58, PAGE_WIDTH - 2 * margin, 58);
    stream.fill();
    drawText(title, margin + 12, top - 23, 14, FONT_BOLD, Color.WHITE);
    drawText(subtitle, margin + 12, top - 42, 9, FONT_REGULAR, accent);
    y -= 70;
  }

  public void addTitle(String text, float size, Color color) throws IOException {
    addText(text, size, color, true, 5);
  }

  public void addSection(String text) throws IOException {
    ensureSpace(30);
    y -= 8;
    drawText(text, margin, y, 10, FONT_BOLD, new Color(32, 58, 67));
    y -= 8;
    stream.setStrokingColor(BORDER);
    stream.moveTo(margin, y);
    stream.lineTo(PAGE_WIDTH - margin, y);
    stream.stroke();
    y -= 12;
  }

  public void addText(String text, float size, Color color, boolean bold, float spacing)
      throws IOException {
    PDType1Font font = bold ? FONT_BOLD : FONT_REGULAR;
    float maxWidth = PAGE_WIDTH - 2 * margin;
    for (String line : wrap(text, font, size, maxWidth)) {
      ensureSpace(size + spacing + 2);
      drawText(line, margin, y, size, font, color);
      y -= size + spacing;
    }
  }

  public void addTable(
      String[] headers,
      List<String[]> rows,
      float[] weights,
      float fontSize,
      Color headerColor,
      Color headerTextColor)
      throws IOException {
    if (headers.length != weights.length) {
      throw new IllegalArgumentException("Header and column weight counts must match");
    }
    drawTableRow(headers, weights, fontSize, headerColor, headerTextColor, true);
    int rowIndex = 0;
    for (String[] row : rows) {
      if (row.length != weights.length) {
        throw new IllegalArgumentException("Table row and column counts must match");
      }
      Color background = rowIndex++ % 2 == 0 ? Color.WHITE : ALT_ROW;
      drawTableRow(row, weights, fontSize, background, Color.DARK_GRAY, false);
    }
    y -= 8;
  }

  public void addImage(BufferedImage image, float width, float height) throws IOException {
    ensureSpace(height + 8);
    var pdfImage = LosslessFactory.createFromImage(document, image);
    stream.drawImage(pdfImage, PAGE_WIDTH - margin - width, y - height, width, height);
    y -= height + 10;
  }

  public byte[] toByteArray() throws IOException {
    if (!saved) {
      closeStream();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      document.save(output);
      saved = true;
      return output.toByteArray();
    }
    throw new IllegalStateException("PDF document was already saved");
  }

  @Override
  public void close() throws IOException {
    closeStream();
    document.close();
  }

  private void drawTableRow(
      String[] values,
      float[] weights,
      float fontSize,
      Color background,
      Color textColor,
      boolean header)
      throws IOException {
    float tableWidth = PAGE_WIDTH - 2 * margin;
    float weightTotal = 0;
    for (float weight : weights) weightTotal += weight;

    List<List<String>> lines = new ArrayList<>(values.length);
    int lineCount = 1;
    for (int i = 0; i < values.length; i++) {
      float cellWidth = tableWidth * weights[i] / weightTotal;
      List<String> cellLines =
          wrap(values[i], header ? FONT_BOLD : FONT_REGULAR, fontSize, Math.max(12, cellWidth - 8));
      lines.add(cellLines);
      lineCount = Math.max(lineCount, Math.min(3, cellLines.size()));
    }

    float rowHeight = Math.max(header ? 28 : 24, lineCount * (fontSize + 2) + 8);
    ensureSpace(rowHeight);
    float x = margin;
    float top = y;
    for (int i = 0; i < values.length; i++) {
      float cellWidth = tableWidth * weights[i] / weightTotal;
      stream.setNonStrokingColor(background);
      stream.addRect(x, top - rowHeight, cellWidth, rowHeight);
      stream.fill();
      stream.setStrokingColor(BORDER);
      stream.addRect(x, top - rowHeight, cellWidth, rowHeight);
      stream.stroke();

      PDType1Font font = header ? FONT_BOLD : FONT_REGULAR;
      List<String> cellLines = lines.get(i);
      float textY =
          top - (rowHeight + Math.min(3, cellLines.size()) * (fontSize + 2)) / 2 + fontSize;
      for (int line = 0; line < Math.min(3, cellLines.size()); line++) {
        String value = fit(cellLines.get(line), font, fontSize, cellWidth - 6);
        drawText(value, x + 3, textY, fontSize, font, textColor);
        textY -= fontSize + 2;
      }
      x += cellWidth;
    }
    y -= rowHeight;
  }

  private List<String> wrap(String text, PDType1Font font, float size, float maxWidth)
      throws IOException {
    String safeText = sanitize(text == null ? "" : text);
    List<String> lines = new ArrayList<>();
    for (String paragraph : safeText.split("\\R", -1)) {
      StringBuilder line = new StringBuilder();
      for (String word : paragraph.split("\\s+")) {
        if (word.isEmpty()) continue;
        String candidate = line.isEmpty() ? word : line + " " + word;
        if (width(candidate, font, size) <= maxWidth) {
          line.setLength(0);
          line.append(candidate);
        } else {
          if (!line.isEmpty()) lines.add(line.toString());
          line.setLength(0);
          line.append(fit(word, font, size, maxWidth));
        }
      }
      if (!line.isEmpty()) lines.add(line.toString());
      if (paragraph.isEmpty()) lines.add("");
    }
    return lines;
  }

  private String fit(String text, PDType1Font font, float size, float maxWidth) throws IOException {
    String value = text;
    while (!value.isEmpty() && width(value, font, size) > maxWidth) {
      value = value.substring(0, value.length() - 1);
    }
    return value;
  }

  private float width(String text, PDType1Font font, float size) throws IOException {
    return font.getStringWidth(text) / 1000 * size;
  }

  private String sanitize(String text) {
    String normalized =
        Normalizer.normalize(text, Normalizer.Form.NFC)
            .replace('\u2014', '-')
            .replace('\u2013', '-')
            .replace('\u00a0', ' ')
            .replace('\u00b7', '-');
    StringBuilder safe = new StringBuilder();
    for (int i = 0; i < normalized.length(); i++) {
      char character = normalized.charAt(i);
      try {
        FONT_REGULAR.getStringWidth(String.valueOf(character));
        safe.append(character);
      } catch (IllegalArgumentException | IOException unsupported) {
        safe.append('?');
      }
    }
    return safe.toString();
  }

  private void drawText(
      String text, float x, float baseline, float size, PDType1Font font, Color color)
      throws IOException {
    stream.beginText();
    stream.setNonStrokingColor(color);
    stream.setFont(font, size);
    stream.newLineAtOffset(x, baseline);
    stream.showText(text);
    stream.endText();
  }

  private void ensureSpace(float height) throws IOException {
    if (y - height < margin) addPage();
  }

  private void addPage() throws IOException {
    closeStream();
    page = new PDPage(PDRectangle.A4);
    document.addPage(page);
    stream = new PDPageContentStream(document, page);
    y = PAGE_HEIGHT - margin;
  }

  private void closeStream() throws IOException {
    if (stream != null) {
      stream.close();
      stream = null;
    }
  }
}
