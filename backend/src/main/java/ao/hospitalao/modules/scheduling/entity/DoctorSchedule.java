package ao.hospitalao.modules.scheduling.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.entity.TenantScopedEntity;
import jakarta.persistence.*;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "doctor_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorSchedule extends TenantScopedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false, insertable = false, updatable = false)
  private Hospital hospital;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "doctor_id", nullable = false)
  private User doctor;

  /** 0=Segunda, 1=Terça, ..., 6=Domingo */
  @Column(name = "day_of_week", nullable = false)
  private Integer dayOfWeek;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  /** Duração de cada slot em minutos */
  @Column(name = "slot_duration_minutes", nullable = false)
  @Builder.Default
  private Integer slotDurationMinutes = 30;

  @Column(name = "max_patients_per_slot", nullable = false)
  @Builder.Default
  private Integer maxPatientsPerSlot = 1;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
  }

  public enum DayOfWeekLabel {
    SEGUNDA,
    TERCA,
    QUARTA,
    QUINTA,
    SEXTA,
    SABADO,
    DOMINGO;

    public static String label(int dow) {
      return switch (dow) {
        case 0 -> "Segunda-feira";
        case 1 -> "Terça-feira";
        case 2 -> "Quarta-feira";
        case 3 -> "Quinta-feira";
        case 4 -> "Sexta-feira";
        case 5 -> "Sábado";
        case 6 -> "Domingo";
        default -> "—";
      };
    }
  }
}
