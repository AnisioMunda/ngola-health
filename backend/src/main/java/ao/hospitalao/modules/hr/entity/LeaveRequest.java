package ao.hospitalao.modules.hr.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "leave_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false)
  private Hospital hospital;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "leave_type", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private LeaveType leaveType;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "total_days", nullable = false)
  private Integer totalDays;

  @Column(name = "reason", length = 500)
  private String reason;

  @Column(name = "status", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private LeaveStatus status = LeaveStatus.PENDING;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by")
  private User approvedBy;

  @Column(name = "approved_at")
  private OffsetDateTime approvedAt;

  @Column(name = "rejection_reason", length = 300)
  private String rejectionReason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
    if (this.totalDays == null && startDate != null && endDate != null) {
      this.totalDays = (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1);
    }
  }

  public enum LeaveType {
    VACATION,
    SICK_LEAVE,
    PERSONAL,
    MATERNITY,
    PATERNITY,
    BEREAVEMENT,
    UNPAID,
    COMPENSATORY
  }

  public enum LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
  }
}
