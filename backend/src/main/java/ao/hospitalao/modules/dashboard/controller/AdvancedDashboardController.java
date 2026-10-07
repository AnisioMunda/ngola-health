package ao.hospitalao.modules.dashboard.controller;

import ao.hospitalao.modules.dashboard.dto.AdvancedDashboardResponse;
import ao.hospitalao.modules.dashboard.service.AdvancedDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard/advanced")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Advanced dashboard with KPIs and trends")
@SecurityRequirement(name = "bearerAuth")
public class AdvancedDashboardController {

    private final AdvancedDashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get advanced dashboard — KPIs, trends, financial metrics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AdvancedDashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}