package ao.hospitalao.modules.pharmacy.repository;

import ao.hospitalao.modules.pharmacy.entity.StockBatch;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, UUID> {

  List<StockBatch> findByMedicationIdAndHospitalId(UUID medicationId, UUID hospitalId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
        SELECT b FROM StockBatch b
        WHERE b.medication.id = :medicationId
        AND b.quantityAvailable > 0
        AND b.expiryDate >= :today
        ORDER BY b.expiryDate ASC, b.id ASC
    """)
  List<StockBatch> findAvailableBatchesFefo(
      @Param("medicationId") UUID medicationId, @Param("today") LocalDate today);

  @Query(
      """
        SELECT COALESCE(SUM(b.quantityAvailable), 0) FROM StockBatch b
        WHERE b.medication.id = :medicationId AND b.expiryDate >= :today
    """)
  Integer getTotalAvailableQuantity(
      @Param("medicationId") UUID medicationId, @Param("today") LocalDate today);

  @Query(
      """
        SELECT b FROM StockBatch b
        WHERE b.medication.hospital.id = :hospitalId
        AND b.quantityAvailable > 0
        AND b.expiryDate BETWEEN :today AND :limitDate
        ORDER BY b.expiryDate ASC
    """)
  List<StockBatch> findExpiringSoon(
      @Param("hospitalId") UUID hospitalId,
      @Param("today") LocalDate today,
      @Param("limitDate") LocalDate limitDate);
}
