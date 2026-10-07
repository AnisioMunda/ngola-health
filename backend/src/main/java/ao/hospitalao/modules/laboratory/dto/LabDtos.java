package ao.hospitalao.modules.laboratory.dto;

import ao.hospitalao.modules.laboratory.entity.LabRequest.Priority;
import ao.hospitalao.modules.laboratory.entity.LabRequest.RequestStatus;
import ao.hospitalao.modules.laboratory.entity.LabTest.TestCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// ============================================================
// LabTest DTOs
// ============================================================

public class LabDtos {

    @Data @Builder
    public static class LabTestResponse {
        private UUID id;
        private String code;
        private String name;
        private TestCategory category;
        private String sampleType;
        private Integer turnaroundHours;
        private BigDecimal price;
        private String referenceValues;
        private boolean active;
    }

    @Data
    public static class CreateLabTestRequest {
        private String code;
        private String name;
        private TestCategory category;
        private String sampleType;
        private Integer turnaroundHours;
        private BigDecimal price;
        private String referenceValues;
    }

    // ============================================================
    // LabRequest DTOs
    // ============================================================

    @Data @Builder
    public static class LabRequestResponse {
        private UUID id;
        private UUID patientId;
        private String patientName;
        private UUID episodeId;
        private UUID requestedById;
        private String requestedByName;
        private RequestStatus status;
        private Priority priority;
        private String clinicalNotes;
        private OffsetDateTime collectedAt;
        private OffsetDateTime completedAt;
        private OffsetDateTime createdAt;
        private List<LabRequestItemResponse> items;
    }

    @Data @Builder
    public static class LabRequestItemResponse {
        private UUID id;
        private UUID labTestId;
        private String testCode;
        private String testName;
        private String resultValue;
        private String resultUnit;
        private String referenceRange;
        private boolean abnormal;
        private String resultNotes;
        private OffsetDateTime resultedAt;
    }

    @Data
    public static class CreateLabRequestRequest {
        private UUID patientId;
        private UUID episodeId;
        private UUID requestedById;
        private Priority priority;
        private String clinicalNotes;
        private List<UUID> labTestIds;
    }

    @Data
    public static class SubmitResultRequest {
        private String resultValue;
        private String resultUnit;
        private String referenceRange;
        private boolean abnormal;
        private String resultNotes;
    }
}