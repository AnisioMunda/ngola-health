package ao.hospitalao.modules.hr.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.inpatient.entity.Ward;
import ao.hospitalao.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "shifts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false, insertable = false, updatable = false)
  private Hospital hospital;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "shift_type", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private ShiftType shiftType = ShiftType.MORNING;

  @Column(name = "shift_date", nullable = false)
  private LocalDate shiftDate;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Column(name = "department", length = 100)
  private String department;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ward_id")
  private Ward ward;

  @Column(name = "status", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private ShiftStatus status = ShiftStatus.SCHEDULED;

  @Column(name = "notes", length = 300)
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
    this.updatedAt = OffsetDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = OffsetDateTime.now();
  }

  /** Duração em horas */
  public double getDurationHours() {
    if (startTime == null || endTime == null) return 0;
    long minutes = java.time.Duration.between(startTime, endTime).toMinutes();
    if (minutes < 0) minutes += 24 * 60; // turno noturno passa da meia-noite
    return minutes / 60.0;
  }

  public enum ShiftType {
    MORNING,
    AFTERNOON,
    NIGHT,
    FULL_DAY,
    ON_CALL
  }

  public enum ShiftStatus {
    SCHEDULED,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    SWAPPED
  }
}
