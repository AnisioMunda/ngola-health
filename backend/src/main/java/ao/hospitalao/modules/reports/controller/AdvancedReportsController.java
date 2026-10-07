package ao.hospitalao.modules.reports.controller;

import ao.hospitalao.modules.reports.dto.ReportsDtos.*;
import ao.hospitalao.modules.reports.service.AdvancedReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports/advanced")
@RequiredArgsConstructor
@Tag(name = "Advanced Reports", description = "Relatórios avançados e dashboard executivo")
@SecurityRequirement(name = "bearerAuth")
public class AdvancedReportsController {

  private final AdvancedReportsService reportsService;

  @GetMapping("/executive")
  @Operation(summary = "Dashboard executivo — KPIs financeiros, clínicos e operacionais")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  public ResponseEntity<ExecutiveDashboardDto> getExecutiveDashboard() {
    return ResponseEntity.ok(reportsService.getExecutiveDashboard());
  }

  @GetMapping("/bed-occupancy")
  @Operation(summary = "Relatório de ocupação de camas por enfermaria")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  public ResponseEntity<BedOccupancyReportDto> getBedOccupancy() {
    return ResponseEntity.ok(reportsService.getBedOccupancy());
  }

  @GetMapping("/financial")
  @Operation(summary = "Relatório financeiro por período")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  public ResponseEntity<FinancialReportDto> getFinancialReport(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ResponseEntity.ok(reportsService.getFinancialReport(from, to));
  }
}
