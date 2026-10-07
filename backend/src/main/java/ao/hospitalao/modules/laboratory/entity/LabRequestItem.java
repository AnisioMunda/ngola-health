package ao.hospitalao.modules.laboratory.entity;

import ao.hospitalao.modules.auth.entity.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_request_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabRequestItem {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id", nullable = false)
  private LabRequest request;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "lab_test_id", nullable = false)
  private LabTest labTest;

  @Column(name = "result_value", columnDefinition = "TEXT")
  private String resultValue;

  @Column(name = "result_unit", length = 30)
  private String resultUnit;

  @Column(name = "reference_range", length = 100)
  private String referenceRange;

  @Column(name = "is_abnormal")
  @Builder.Default
  private boolean abnormal = false;

  @Column(name = "result_notes", columnDefinition = "TEXT")
  private String resultNotes;

  @Column(name = "resulted_at")
  private OffsetDateTime resultedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resulted_by")
  private User resultedBy;
}
