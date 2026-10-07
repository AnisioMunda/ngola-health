package ao.hospitalao.modules.prescription.entity;

import ao.hospitalao.modules.pharmacy.entity.Medication;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "prescription_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prescription_id", nullable = false)
  private Prescription prescription;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "medication_id", nullable = false)
  private Medication medication;

  @Column(name = "quantity_prescribed", nullable = false)
  private Integer quantityPrescribed;

  @Column(name = "quantity_dispensed", nullable = false)
  @Builder.Default
  private Integer quantityDispensed = 0;

  @Column(name = "dosage", nullable = false, length = 200)
  private String dosage;

  @Column(name = "frequency_hours")
  private Integer frequencyHours;

  @Column(name = "duration_days")
  private Integer durationDays;

  @Column(name = "route", length = 50)
  private String route;

  @Column(name = "instructions", length = 300)
  private String instructions;

  @Column(name = "status", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private ItemStatus status = ItemStatus.PENDING;

  public int getRemainingQuantity() {
    return quantityPrescribed - quantityDispensed;
  }

  public boolean isFullyDispensed() {
    return quantityDispensed >= quantityPrescribed;
  }

  public enum ItemStatus {
    PENDING,
    DISPENSED,
    PARTIAL,
    CANCELLED
  }
}
