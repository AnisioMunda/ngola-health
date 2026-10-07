package ao.hospitalao.modules.equipment.repository;

import ao.hospitalao.modules.equipment.entity.Equipment;
import ao.hospitalao.modules.equipment.entity.Equipment.EquipmentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {

  Page<Equipment> findByHospitalIdAndActiveTrue(UUID hospitalId, Pageable pageable);

  List<Equipment> findByHospitalIdAndStatus(UUID hospitalId, EquipmentStatus status);

  @Query(
      """
        SELECT e FROM Equipment e
        LEFT JOIN FETCH e.ward
        WHERE e.hospital.id = :hospitalId
        AND   e.active = true
        AND   e.nextMaintenanceDate <= :date
        ORDER BY e.nextMaintenanceDate ASC
    """)
  List<Equipment> findMaintenanceDue(
      @Param("hospitalId") UUID hospitalId, @Param("date") LocalDate date);

  @Query(
      """
        SELECT e FROM Equipment e
        WHERE e.hospital.id = :hospitalId
        AND   e.active = true
        AND   e.nextCalibrationDate <= :date
        ORDER BY e.nextCalibrationDate ASC
    """)
  List<Equipment> findCalibrationDue(
      @Param("hospitalId") UUID hospitalId, @Param("date") LocalDate date);

  @Query(
      """
        SELECT e FROM Equipment e
        WHERE e.hospital.id = :hospitalId
        AND   e.active = true
        AND   (LOWER(e.name)  LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(e.code)  LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(e.brand) LIKE LOWER(CONCAT('%', :q, '%')))
    """)
  Page<Equipment> search(
      @Param("hospitalId") UUID hospitalId, @Param("q") String query, Pageable pageable);

  long countByHospitalIdAndActiveTrue(UUID hospitalId);

  long countByHospitalIdAndStatus(UUID hospitalId, EquipmentStatus status);
}
