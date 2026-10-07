// ============================================================
// WardRepository.java
// ============================================================
package ao.hospitalao.modules.inpatient.repository;
 
import ao.hospitalao.modules.inpatient.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
@Repository
public interface WardRepository extends JpaRepository<Ward, UUID> {
 
    List<Ward> findByHospitalIdAndActiveTrueOrderByName(UUID hospitalId);
 
    Optional<Ward> findByHospitalIdAndCode(UUID hospitalId, String code);
 
    boolean existsByHospitalIdAndCode(UUID hospitalId, String code);
 
    @Query("""
        SELECT w FROM Ward w
        LEFT JOIN FETCH w.beds b
        LEFT JOIN FETCH w.responsibleDoctor
        WHERE w.hospital.id = :hospitalId
        AND w.active = true
        ORDER BY w.name
    """)
    List<Ward> findWithBedsAndDoctor(@Param("hospitalId") UUID hospitalId);
}