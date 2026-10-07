package ao.hospitalao.modules.telemedicine.repository;
 
import ao.hospitalao.modules.telemedicine.entity.TelemedicineSession;
import ao.hospitalao.modules.telemedicine.entity.TelemedicineSession.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
@Repository
public interface TelemedicineRepository extends JpaRepository<TelemedicineSession, UUID> {
 
    Optional<TelemedicineSession> findByRoomToken(String roomToken);
 
    @Query("""
        SELECT s FROM TelemedicineSession s
        LEFT JOIN FETCH s.patient
        LEFT JOIN FETCH s.doctor
        WHERE s.hospital.id = :hospitalId
        AND   s.status IN ('SCHEDULED','WAITING','IN_PROGRESS')
        ORDER BY s.scheduledAt ASC
    """)
    List<TelemedicineSession> findActiveByHospital(@Param("hospitalId") UUID hospitalId);
 
    @Query("""
        SELECT s FROM TelemedicineSession s
        LEFT JOIN FETCH s.patient
        LEFT JOIN FETCH s.doctor
        WHERE s.doctor.id = :doctorId
        AND   s.scheduledAt >= :from
        ORDER BY s.scheduledAt ASC
    """)
    List<TelemedicineSession> findByDoctorAndDate(
        @Param("doctorId") UUID doctorId,
        @Param("from") OffsetDateTime from
    );
 
    @Query("""
        SELECT s FROM TelemedicineSession s
        LEFT JOIN FETCH s.doctor
        WHERE s.patient.id = :patientId
        ORDER BY s.scheduledAt DESC
    """)
    Page<TelemedicineSession> findByPatient(
        @Param("patientId") UUID patientId, Pageable pageable);
 
    long countByHospitalIdAndStatus(UUID hospitalId, SessionStatus status);
}