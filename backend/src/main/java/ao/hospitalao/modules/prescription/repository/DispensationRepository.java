 // ============================================================
// DispensationRepository.java
// ============================================================
package ao.hospitalao.modules.prescription.repository;
 
import ao.hospitalao.modules.prescription.entity.Dispensation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.UUID;
 
@Repository
public interface DispensationRepository extends JpaRepository<Dispensation, UUID> {
 
    List<Dispensation> findByPrescriptionItemId(UUID prescriptionItemId);
 
    @Query("""
        SELECT d FROM Dispensation d
        LEFT JOIN FETCH d.medication
        LEFT JOIN FETCH d.stockBatch
        LEFT JOIN FETCH d.dispensedBy
        WHERE d.prescriptionItem.prescription.id = :prescriptionId
        ORDER BY d.dispensedAt DESC
    """)
    List<Dispensation> findByPrescriptionId(@Param("prescriptionId") UUID prescriptionId);
}