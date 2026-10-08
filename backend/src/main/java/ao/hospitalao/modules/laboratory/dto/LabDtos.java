package ao.hospitalao.modules.laboratory.dto;

import ao.hospitalao.modules.laboratory.entity.LabRequest.Priority;
import ao.hospitalao.modules.laboratory.entity.LabRequest.RequestStatus;
import ao.hospitalao.modules.laboratory.entity.LabTest.TestCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

// ============================================================
// LabTest DTOs
// ============================================================

public class LabDtos {

  @Data
  @Builder
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
    @NotBlank
    @Size(max = 30)
    private String code;

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull private TestCategory category;

    @Size(max = 50)
    private String sampleType;

    @Min(1)
    private Integer turnaroundHours;

    @DecimalMin("0.00")
    private BigDecimal price;

    private String referenceValues;
  }

  // ============================================================
  // LabRequest DTOs
  // ============================================================

  @Data
  @Builder
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

  @Data
  @Builder
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
    @NotNull private UUID patientId;

    private UUID episodeId;

    private UUID requestedById;

    private Priority priority;

    private String clinicalNotes;

    @NotEmpty private List<@NotNull UUID> labTestIds;
  }

  @Data
  public static class SubmitResultRequest {
    @NotBlank private String resultValue;

    @Size(max = 30)
    private String resultUnit;

    @Size(max = 100)
    private String referenceRange;

    private boolean abnormal;

    private String resultNotes;
  }
}
