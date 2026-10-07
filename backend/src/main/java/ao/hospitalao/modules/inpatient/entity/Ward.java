package ao.hospitalao.modules.inpatient.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.entity.TenantScopedEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "wards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ward extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false, insertable = false, updatable = false)
  private Hospital hospital;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "code", nullable = false, length = 20)
  private String code;

  @Column(name = "type", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private WardType type = WardType.GENERAL;

  @Column(name = "floor", length = 10)
  private String floor;

  @Column(name = "total_beds", nullable = false)
  @Builder.Default
  private Integer totalBeds = 0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "responsible_doctor_id")
  private User responsibleDoctor;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @OneToMany(mappedBy = "ward", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Bed> beds = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
  }

  public enum WardType {
    GENERAL,
    PEDIATRIC,
    MATERNITY,
    ICU,
    SURGICAL,
    CARDIOLOGY,
    ONCOLOGY,
    EMERGENCY,
    ISOLATION
  }
}
