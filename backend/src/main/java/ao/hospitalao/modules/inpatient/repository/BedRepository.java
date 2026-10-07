// ============================================================
// BedRepository.java
// ============================================================
package ao.hospitalao.modules.inpatient.repository;

import ao.hospitalao.modules.inpatient.entity.Bed;
import ao.hospitalao.modules.inpatient.entity.Bed.BedStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BedRepository extends JpaRepository<Bed, UUID> {

  List<Bed> findByWardIdAndActiveTrueOrderByBedNumber(UUID wardId);

  List<Bed> findByWardIdAndStatusAndActiveTrue(UUID wardId, BedStatus status);

  List<Bed> findByHospitalIdAndStatusAndActiveTrue(UUID hospitalId, BedStatus status);

  @Query(
      """
        SELECT COUNT(b) FROM Bed b
        WHERE b.ward.id = :wardId
        AND b.status = :status
        AND b.active = true
    """)
  long countByWardAndStatus(@Param("wardId") UUID wardId, @Param("status") BedStatus status);

  @Query(
      """
        SELECT COUNT(b) FROM Bed b
        WHERE b.hospital.id = :hospitalId
        AND b.status = 'AVAILABLE'
        AND b.active = true
    """)
  long countAvailableByHospital(@Param("hospitalId") UUID hospitalId);

  @Query(
      """
        SELECT COUNT(b) FROM Bed b
        WHERE b.hospital.id = :hospitalId
        AND b.active = true
    """)
  long countTotalByHospital(@Param("hospitalId") UUID hospitalId);
}
