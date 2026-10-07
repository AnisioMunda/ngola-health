package ao.hospitalao.modules.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {

    // Totais gerais
    private long totalPatients;
    private long totalUsers;
    private long activeUsers;

    // Pacientes por género
    private long malePatients;
    private long femalePatients;

    // Pacientes registados nos últimos 7 dias
    private long newPatientsThisWeek;

    // Distribuição de utilizadores por role
    private Map<String, Long> usersByRole;

    // Distribuição de pacientes por província (top 5)
    private List<ProvinceCount> patientsByProvince;

    // Registos recentes (últimos 5 pacientes)
    private List<RecentPatient> recentPatients;

    @Data
    @Builder
    public static class ProvinceCount {
        private String province;
        private long count;
    }

    @Data
    @Builder
    public static class RecentPatient {
        private String id;
        private String fullName;
        private String createdAt;
    }
}