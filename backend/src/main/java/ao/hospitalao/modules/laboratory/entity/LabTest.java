package ao.hospitalao.modules.laboratory.entity;

import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_tests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabTest extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false, insertable = false, updatable = false)
  private Hospital hospital;

  @Column(name = "code", nullable = false, length = 30)
  private String code;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "category", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private TestCategory category;

  @Column(name = "sample_type", length = 50)
  private String sampleType;

  @Column(name = "turnaround_hours")
  @Builder.Default
  private Integer turnaroundHours = 24;

  @Column(name = "price", precision = 10, scale = 2)
  private BigDecimal price;

  @Column(name = "reference_values", columnDefinition = "TEXT")
  private String referenceValues;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
  }

  public enum TestCategory {
    HEMATOLOGY,
    BIOCHEMISTRY,
    MICROBIOLOGY,
    IMMUNOLOGY,
    URINE,
    IMAGING,
    CARDIOLOGY,
    OTHER
  }
}
