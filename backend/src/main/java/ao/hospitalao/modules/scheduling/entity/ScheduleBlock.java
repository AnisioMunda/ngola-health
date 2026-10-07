package ao.hospitalao.modules.scheduling.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "schedule_blocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleBlock {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "doctor_id", nullable = false)
  private User doctor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false)
  private Hospital hospital;

  @Column(name = "block_date", nullable = false)
  private LocalDate blockDate;

  @Column(name = "start_time")
  private LocalTime startTime;

  @Column(name = "end_time")
  private LocalTime endTime;

  /** true = bloqueia o dia inteiro */
  @Column(name = "all_day", nullable = false)
  @Builder.Default
  private boolean allDay = false;

  @Column(name = "reason", length = 200)
  private String reason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
  }
}
