package ao.hospitalao.modules.prescription.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.pharmacy.entity.Medication;
import ao.hospitalao.modules.pharmacy.entity.StockBatch;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dispensations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispensation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_item_id", nullable = false)
    private PrescriptionItem prescriptionItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_batch_id", nullable = false)
    private StockBatch stockBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispensed_by", nullable = false)
    private User dispensedBy;

    @Column(name = "quantity_dispensed", nullable = false)
    private Integer quantityDispensed;

    @Column(name = "dispensed_at", nullable = false)
    private OffsetDateTime dispensedAt;

    @Column(name = "notes", length = 300)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (this.dispensedAt == null)
            this.dispensedAt = OffsetDateTime.now();
    }
}