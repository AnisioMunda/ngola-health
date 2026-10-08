package ao.hospitalao.modules.hr.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "attendance_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord extends TenantScopedEntity {

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shift_id")
  private Shift shift;

  @Column(name = "work_date", nullable = false)
  private LocalDate workDate;

  @Column(name = "check_in")
  private OffsetDateTime checkIn;

  @Column(name = "check_out")
  private OffsetDateTime checkOut;

  @Column(name = "minutes_worked")
  private Integer minutesWorked;

  @Column(name = "overtime_minutes")
  @Builder.Default
  private Integer overtimeMinutes = 0;

  @Column(name = "status", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private AttendanceStatus status = AttendanceStatus.PRESENT;

  @Column(name = "notes", length = 300)
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
  }

  /** Calcular minutos trabalhados ao fazer check-out */
  public void calculateMinutesWorked() {
    if (checkIn != null && checkOut != null) {
      long minutes = java.time.Duration.between(checkIn, checkOut).toMinutes();
      this.minutesWorked = (int) Math.max(0, minutes);
    }
  }

  /** Horas trabalhadas formatadas */
  public String getHoursWorked() {
    if (minutesWorked == null) return "—";
    return minutesWorked / 60 + "h" + minutesWorked % 60 + "m";
  }

  public enum AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE,
    ON_LEAVE,
    HOLIDAY
  }
}
