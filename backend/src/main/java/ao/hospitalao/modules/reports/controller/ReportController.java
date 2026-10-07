package ao.hospitalao.modules.reports.controller;

import ao.hospitalao.modules.reports.service.PdfReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "PDF report generation")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final PdfReportService pdfReportService;

    @GetMapping("/patients/{id}")
    @Operation(summary = "Generate patient clinical record PDF")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','MANAGER')")
    public ResponseEntity<byte[]> patientReport(@PathVariable UUID id) throws IOException {
        byte[] pdf = pdfReportService.generatePatientReport(id);
        return buildPdfResponse(pdf, "patient-report-" + id + ".pdf");
    }

    @GetMapping("/stock")
    @Operation(summary = "Generate pharmacy stock report PDF")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST','MANAGER')")
    public ResponseEntity<byte[]> stockReport() throws IOException {
        byte[] pdf = pdfReportService.generateStockReport();
        return buildPdfResponse(pdf, "stock-report-" + LocalDate.now() + ".pdf");
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
            ContentDisposition.attachment().filename(filename).build()
        );
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}