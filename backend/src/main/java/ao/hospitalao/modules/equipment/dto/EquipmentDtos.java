package ao.hospitalao.modules.equipment.dto;
 
import ao.hospitalao.modules.equipment.entity.Equipment.EquipmentCategory;
import ao.hospitalao.modules.equipment.entity.Equipment.EquipmentStatus;
import ao.hospitalao.modules.equipment.entity.MaintenanceRecord.MaintenanceType;
import lombok.Builder;
import lombok.Data;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
 
public class EquipmentDtos {
 
    @Data @Builder
    public static class EquipmentResponse {
        private UUID              id;
        private String            name;
        private String            code;
        private String            brand;
        private String            model;
        private String            serialNumber;
        private EquipmentCategory category;
        private String            categoryLabel;
        private String            location;
        private String            wardName;
        private EquipmentStatus   status;
        private String            statusLabel;
        private LocalDate         purchaseDate;
        private BigDecimal        purchasePrice;
        private LocalDate         warrantyExpiry;
        private boolean           warrantyExpired;
        private LocalDate         nextMaintenanceDate;
        private boolean           maintenanceDue;
        private LocalDate         nextCalibrationDate;
        private Integer           maintenanceIntervalDays;
        private String            notes;
        private List<MaintenanceResponse> maintenanceHistory;
        private OffsetDateTime    createdAt;
    }
 
    @Data
    public static class CreateEquipmentRequest {
        private String            name;
        private String            code;
        private String            brand;
        private String            model;
        private String            serialNumber;
        private EquipmentCategory category;
        private String            location;
        private UUID              wardId;
        private LocalDate         purchaseDate;
        private BigDecimal        purchasePrice;
        private LocalDate         warrantyExpiry;
        private LocalDate         nextMaintenanceDate;
        private Integer           maintenanceIntervalDays;
        private LocalDate         nextCalibrationDate;
        private String            notes;
    }
 
    @Data
    public static class UpdateStatusRequest {
        private EquipmentStatus status;
        private String          notes;
    }
 
    @Data @Builder
    public static class MaintenanceResponse {
        private UUID            id;
        private MaintenanceType type;
        private String          typeLabel;
        private String          performedByName;
        private OffsetDateTime  performedAt;
        private String          description;
        private BigDecimal      cost;
        private LocalDate       nextMaintenanceDate;
        private String          partsReplaced;
        private String          result;
    }
 
    @Data
    public static class CreateMaintenanceRequest {
        private MaintenanceType type;
        private String          description;
        private BigDecimal      cost;
        private LocalDate       nextMaintenanceDate;
        private String          partsReplaced;
        private String          result;
    }
 
    @Data @Builder
    public static class EquipmentStatsDto {
        private long totalEquipment;
        private long activeEquipment;
        private long inMaintenance;
        private long maintenanceDue;
        private long calibrationDue;
        private long warrantyExpired;
    }
}