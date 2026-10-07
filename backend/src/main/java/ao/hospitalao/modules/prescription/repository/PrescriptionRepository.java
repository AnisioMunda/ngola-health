// ============================================================
// PrescriptionRepository.java
// ============================================================
package ao.hospitalao.modules.prescription.repository;
 
import ao.hospitalao.modules.prescription.entity.Prescription;
import ao.hospitalao.modules.prescription.entity.Prescription.PrescriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
 
    // Historial de prescrições de um paciente
    Page<Prescription> findByPatientIdOrderByPrescriptionDateDesc(
        UUID patientId, Pageable pageable);
 
    // Prescrições activas de um paciente
    List<Prescription> findByPatientIdAndStatus(UUID patientId, PrescriptionStatus status);
 
    // Prescrições por episódio
    List<Prescription> findByEpisodeIdOrderByCreatedAtDesc(UUID episodeId);
 
    // Prescrições por internamento
    List<Prescription> findByAdmissionIdOrderByCreatedAtDesc(UUID admissionId);
 
    // Com relações para detalhe
    @Query("""
        SELECT p FROM Prescription p
        LEFT JOIN FETCH p.patient
        LEFT JOIN FETCH p.doctor
        LEFT JOIN FETCH p.episode
        LEFT JOIN FETCH p.items i
        LEFT JOIN FETCH i.medication
        WHERE p.id = :id
    """)
    Optional<Prescription> findByIdWithRelations(@Param("id") UUID id);
 
    // Prescrições do hospital num período
    @Query("""
        SELECT p FROM Prescription p
        LEFT JOIN FETCH p.patient
        LEFT JOIN FETCH p.doctor
        WHERE p.hospital.id      = :hospitalId
        AND   p.prescriptionDate BETWEEN :from AND :to
        ORDER BY p.prescriptionDate DESC
    """)
    Page<Prescription> findByHospitalAndPeriod(
        @Param("hospitalId") UUID hospitalId,
        @Param("from")       LocalDate from,
        @Param("to")         LocalDate to,
        Pageable pageable
    );
 
    // Prescrições activas a expirar em breve
    @Query("""
        SELECT p FROM Prescription p
        LEFT JOIN FETCH p.patient
        WHERE p.hospital.id = :hospitalId
        AND   p.status      = 'ACTIVE'
        AND   p.expiryDate  <= :expiryBefore
        ORDER BY p.expiryDate ASC
    """)
    List<Prescription> findExpiringBefore(
        @Param("hospitalId")    UUID hospitalId,
        @Param("expiryBefore")  LocalDate expiryBefore
    );
 
    // Número único sequencial
    @Query(value = "SELECT nextval('prescription_number_seq')", nativeQuery = true)
    long nextPrescriptionNumber();
}