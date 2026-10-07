// ============================================================
// PrescriptionItemRepository.java
// ============================================================
package ao.hospitalao.modules.prescription.repository;

import ao.hospitalao.modules.prescription.entity.PrescriptionItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {

  List<PrescriptionItem> findByPrescriptionId(UUID prescriptionId);

  List<PrescriptionItem> findByMedicationId(UUID medicationId);
}
