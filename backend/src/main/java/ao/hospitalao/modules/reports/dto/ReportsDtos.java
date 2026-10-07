package ao.hospitalao.modules.reports.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

public class ReportsDtos {

  // ============================================================
  // Dashboard Executivo
  // ============================================================

  @Data
  @Builder
  public static class ExecutiveDashboardDto {
    // Financeiro
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueLastMonth;
    private double revenueGrowthPercent;
    private BigDecimal revenueYTD;
    private long invoicesThisMonth;
    private long invoicesPending;

    // Clínico
    private long totalPatients;
    private long newPatientsThisMonth;
    private long episodesToday;
    private long episodesThisMonth;
    private long admissionsActive;
    private long bedsAvailable;
    private long bedsTotal;
    private double occupancyRate;

    // Operacional
    private long appointmentsToday;
    private long labRequestsThisMonth;
    private long prescriptionsThisMonth;
    private long triageToday;
    private long pendingLeaves;

    // Tendências mensais (últimos 12 meses)
    private List<MonthlyDataPoint> revenueByMonth;
    private List<MonthlyDataPoint> patientsByMonth;
    private List<MonthlyDataPoint> episodesByMonth;

    // Top médicos
    private List<DoctorStats> topDoctors;

    // Distribuição de episódios por tipo
    private List<CategoryDataPoint> episodesByType;

    // Distribuição de triagem por prioridade
    private List<CategoryDataPoint> triageByPriority;
  }

  @Data
  @Builder
  public static class MonthlyDataPoint {
    private String month; // "Jan", "Fev", ...
    private String monthKey; // "2026-01"
    private BigDecimal value;
    private long count;
  }

  @Data
  @Builder
  public static class CategoryDataPoint {
    private String label;
    private long count;
    private double percentage;
    private String color;
  }

  @Data
  @Builder
  public static class DoctorStats {
    private String doctorName;
    private long episodes;
    private long appointments;
    private long prescriptions;
  }

  // ============================================================
  // Relatório de Ocupação de Camas
  // ============================================================

  @Data
  @Builder
  public static class BedOccupancyReportDto {
    private long totalBeds;
    private long occupiedBeds;
    private long availableBeds;
    private double occupancyRate;
    private double avgStayDays;
    private List<WardOccupancy> byWard;
  }

  @Data
  @Builder
  public static class WardOccupancy {
    private String wardName;
    private String wardType;
    private int totalBeds;
    private int occupiedBeds;
    private double occupancyRate;
    private double avgStayDays;
  }

  // ============================================================
  // Relatório Financeiro Mensal
  // ============================================================

  @Data
  @Builder
  public static class FinancialReportDto {
    private String period;
    private BigDecimal totalRevenue;
    private BigDecimal paidRevenue;
    private BigDecimal pendingRevenue;
    private long totalInvoices;
    private long paidInvoices;
    private long pendingInvoices;
    private List<MonthlyDataPoint> revenueByMonth;
    private List<CategoryDataPoint> revenueByService;
  }

  // ============================================================
  // Relatório de Produtividade Médica
  // ============================================================

  @Data
  @Builder
  public static class DoctorProductivityReportDto {
    private String period;
    private List<DoctorProductivity> doctors;
  }

  @Data
  @Builder
  public static class DoctorProductivity {
    private String doctorName;
    private String specialty;
    private long totalEpisodes;
    private long totalAppointments;
    private long completedAppointments;
    private long totalPrescriptions;
    private double avgAppointmentsPerDay;
  }

  // ============================================================
  // Relatório de RH
  // ============================================================

  @Data
  @Builder
  public static class HrReportDto {
    private String period;
    private long totalStaff;
    private long totalShifts;
    private long totalHoursWorked;
    private long totalOvertimeMinutes;
    private long totalLeavesDays;
    private long absentDays;
    private double attendanceRate;
    private List<StaffAttendance> byStaff;
  }

  @Data
  @Builder
  public static class StaffAttendance {
    private String staffName;
    private String role;
    private long presentDays;
    private long absentDays;
    private long leaveDays;
    private long hoursWorked;
    private long overtimeMinutes;
  }
}
