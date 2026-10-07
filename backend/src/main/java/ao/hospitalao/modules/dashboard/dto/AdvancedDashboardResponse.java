package ao.hospitalao.modules.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdvancedDashboardResponse {

  private long totalPatients;
  private long newPatientsThisMonth;
  private long newPatientsThisWeek;
  private long totalEpisodes;
  private long activeEpisodes;
  private long scheduledToday;
  private long completedToday;
  private long malePatients;
  private long femalePatients;

  private BigDecimal revenueThisMonth;
  private BigDecimal revenueLastMonth;
  private BigDecimal revenueThisYear;
  private BigDecimal pendingAmount;
  private long invoicesThisMonth;
  private long paidInvoicesThisMonth;
  private long pendingInvoices;
  private double collectionRate;

  private long labRequestsThisMonth;
  private long labRequestsPending;
  private long lowStockMedications;
  private long expiringMedications;

  private List<MonthlyRevenue> revenueByMonth;
  private List<MonthlyPatients> patientsByMonth;
  private List<MonthlyEpisodes> episodesByMonth;

  private Map<String, Long> episodesByType;
  private Map<String, Long> usersByRole;
  private List<ProvinceCount> patientsByProvince;
  private List<RecentInvoice> recentInvoices;
  private List<RecentPatient> recentPatients;

  @Data
  @Builder
  public static class MonthlyRevenue {
    private String month;
    private String yearMonth;
    private BigDecimal revenue;
    private BigDecimal paid;
    private long invoiceCount;
  }

  @Data
  @Builder
  public static class MonthlyPatients {
    private String month;
    private String yearMonth;
    private long newPatients;
    private long totalEpisodes;
  }

  @Data
  @Builder
  public static class MonthlyEpisodes {
    private String month;
    private String yearMonth;
    private long scheduled;
    private long completed;
    private long cancelled;
  }

  @Data
  @Builder
  public static class ProvinceCount {
    private String province;
    private long count;
  }

  @Data
  @Builder
  public static class RecentInvoice {
    private String id;
    private String invoiceNumber;
    private String patientName;
    private BigDecimal totalAmount;
    private String status;
    private String createdAt;
  }

  @Data
  @Builder
  public static class RecentPatient {
    private String id;
    private String fullName;
    private String createdAt;
  }
}
