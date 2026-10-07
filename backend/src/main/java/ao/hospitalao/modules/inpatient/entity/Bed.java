package ao.hospitalao.modules.inpatient.entity;

import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.entity.TenantScopedEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "beds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bed extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ward_id", nullable = false)
  private Ward ward;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false, insertable = false, updatable = false)
  private Hospital hospital;

  @Column(name = "bed_number", nullable = false, length = 10)
  private String bedNumber;

  @Column(name = "status", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private BedStatus status = BedStatus.AVAILABLE;

  @Column(name = "type", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private BedType type = BedType.STANDARD;

  @Column(name = "notes", length = 300)
  private String notes;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private boolean active = true;

  public enum BedStatus {
    AVAILABLE,
    OCCUPIED,
    MAINTENANCE,
    RESERVED
  }

  public enum BedType {
    STANDARD,
    PRIVATE,
    SEMI_PRIVATE,
    ICU,
    ISOLATION
  }
}
