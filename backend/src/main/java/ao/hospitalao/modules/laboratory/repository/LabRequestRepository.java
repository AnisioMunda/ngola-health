package ao.hospitalao.modules.laboratory.repository;

import ao.hospitalao.modules.laboratory.entity.LabRequest;
import ao.hospitalao.modules.laboratory.entity.LabRequest.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface LabRequestRepository extends JpaRepository<LabRequest, UUID> {

    Page<LabRequest> findByHospitalId(UUID hospitalId, Pageable pageable);

    Page<LabRequest> findByHospitalIdAndStatus(
        UUID hospitalId, RequestStatus status, Pageable pageable);

    Page<LabRequest> findByPatientId(UUID patientId, Pageable pageable);

    @Query("""
        SELECT r FROM LabRequest r
        WHERE r.hospital.id = :hospitalId
        AND (:patientId IS NULL OR r.patient.id = :patientId)
        AND (:status    IS NULL OR r.status      = :status)
        ORDER BY r.createdAt DESC
    """)
    Page<LabRequest> findWithFilters(
        @Param("hospitalId") UUID hospitalId,
        @Param("patientId")  UUID patientId,
        @Param("status")     RequestStatus status,
        Pageable pageable
    );

    // ------------------------------------------------
    // Dashboard avançado
    // ------------------------------------------------

    @Query("""
        SELECT COUNT(r) FROM LabRequest r
        WHERE r.hospital.id = :hospitalId
        AND r.createdAt >= :since
    """)
    long countByHospitalIdAndCreatedAtAfter(
        @Param("hospitalId") UUID hospitalId,
        @Param("since") OffsetDateTime since
    );

    @Query("""
        SELECT COUNT(r) FROM LabRequest r
        WHERE r.hospital.id = :hospitalId
        AND r.status = :status
    """)
    long countByHospitalIdAndStatus(
        @Param("hospitalId") UUID hospitalId,
        @Param("status") RequestStatus status
    );
}