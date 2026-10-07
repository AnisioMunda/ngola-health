// ============================================================
// AdmissionRepository.java
// ============================================================
package ao.hospitalao.modules.inpatient.repository;
 
import ao.hospitalao.modules.inpatient.entity.Admission;
import ao.hospitalao.modules.inpatient.entity.Admission.AdmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
@Repository
public interface AdmissionRepository extends JpaRepository<Admission, UUID> {
 
    // Internamentos activos por hospital
    List<Admission> findByHospitalIdAndStatusOrderByAdmissionDateDesc(
        UUID hospitalId, AdmissionStatus status);
 
    // Histórico de internamentos de um paciente
    Page<Admission> findByPatientIdOrderByAdmissionDateDesc(
        UUID patientId, Pageable pageable);
 
    // Internamento activo de um paciente
    Optional<Admission> findByPatientIdAndStatus(
        UUID patientId, AdmissionStatus status);
 
    // Internamentos por enfermaria
    List<Admission> findByWardIdAndStatus(UUID wardId, AdmissionStatus status);
 
    // Verificar se cama está ocupada
    @Query("""
        SELECT COUNT(a) > 0 FROM Admission a
        WHERE a.bed.id = :bedId
        AND a.status = 'ACTIVE'
    """)
    boolean isBedOccupied(@Param("bedId") UUID bedId);
 
    // Com todas as relações para detalhe
    @Query("""
        SELECT a FROM Admission a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH a.bed
        LEFT JOIN FETCH a.ward
        LEFT JOIN FETCH a.responsibleDoctor
        LEFT JOIN FETCH a.admittedBy
        LEFT JOIN FETCH a.episode
        WHERE a.id = :id
    """)
    Optional<Admission> findByIdWithRelations(@Param("id") UUID id);
 
    // KPI — total de internamentos activos
    long countByHospitalIdAndStatus(UUID hospitalId, AdmissionStatus status);
 
    // Para o dashboard — internamentos com alta esperada hoje ou passada
    @Query("""
        SELECT a FROM Admission a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH a.bed
        LEFT JOIN FETCH a.ward
        WHERE a.hospital.id = :hospitalId
        AND a.status = 'ACTIVE'
        AND a.expectedDischargeDate <= CURRENT_DATE
        ORDER BY a.expectedDischargeDate ASC
    """)
    List<Admission> findOverdueDischarges(@Param("hospitalId") UUID hospitalId);
 
    // Filtros para lista
    @Query("""
        SELECT a FROM Admission a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH a.bed
        LEFT JOIN FETCH a.ward
        LEFT JOIN FETCH a.responsibleDoctor
        WHERE a.hospital.id = :hospitalId
        AND (:status  IS NULL OR a.status    = :status)
        AND (:wardId  IS NULL OR a.ward.id   = :wardId)
        ORDER BY a.admissionDate DESC
    """)
    Page<Admission> findWithFilters(
        @Param("hospitalId") UUID hospitalId,
        @Param("status")     AdmissionStatus status,
        @Param("wardId")     UUID wardId,
        Pageable pageable
    );
}