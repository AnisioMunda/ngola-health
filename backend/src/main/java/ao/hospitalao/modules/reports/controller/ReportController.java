package ao.hospitalao.modules.reports.controller;

import ao.hospitalao.modules.reports.service.PdfReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Geração de documentos PDF")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

  private static final ZoneId ANGOLA_ZONE = ZoneId.of("Africa/Luanda");

  private final PdfReportService pdfReportService;

  @GetMapping("/patients/{id}")
  @Operation(summary = "Gerar ficha clínica do paciente em PDF")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).DOCTOR.name(),T(ao.hospitalao.security.RoleName).NURSE.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<byte[]> patientReport(@PathVariable UUID id) throws IOException {
    byte[] pdf = pdfReportService.generatePatientReport(id);
    return buildPdfResponse(pdf, "ficha-paciente-" + id + ".pdf");
  }

  @GetMapping("/stock")
  @Operation(summary = "Gerar relatório de stock da farmácia em PDF")
  @PreAuthorize(
      "hasAnyRole(T(ao.hospitalao.security.RoleName).ADMIN.name(),T(ao.hospitalao.security.RoleName).PHARMACIST.name(),T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<byte[]> stockReport() throws IOException {
    byte[] pdf = pdfReportService.generateStockReport();
    return buildPdfResponse(pdf, "inventario-stock-" + LocalDate.now(ANGOLA_ZONE) + ".pdf");
  }

  private ResponseEntity<byte[]> buildPdfResponse(byte[] pdf, String filename) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
    headers.setContentLength(pdf.length);
    return ResponseEntity.ok().headers(headers).body(pdf);
  }
}
