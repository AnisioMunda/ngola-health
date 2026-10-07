package ao.hospitalao.modules.pharmacy.repository;

import ao.hospitalao.modules.pharmacy.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    Page<StockMovement> findByBatchMedicationIdOrderByCreatedAtDesc(UUID medicationId, Pageable pageable);

    Page<StockMovement> findByPatientId(UUID patientId, Pageable pageable);
}