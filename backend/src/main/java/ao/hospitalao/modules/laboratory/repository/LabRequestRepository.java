package ao.hospitalao.modules.laboratory.repository;

import ao.hospitalao.modules.laboratory.application.PatientLabResultProjection;
import ao.hospitalao.modules.laboratory.entity.LabRequest;
import ao.hospitalao.modules.laboratory.entity.LabRequest.RequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabRequestRepository extends JpaRepository<LabRequest, UUID> {

  Page<LabRequest> findByHospitalId(UUID hospitalId, Pageable pageable);

  Page<LabRequest> findByHospitalIdAndStatus(
      UUID hospitalId, RequestStatus status, Pageable pageable);

  Page<LabRequest> findByPatientId(UUID patientId, Pageable pageable);

  Page<LabRequest> findByPatientIdAndStatusOrderByCompletedAtDesc(
      UUID patientId, RequestStatus status, Pageable pageable);

  @Query(
      """
        SELECT item.id AS id,
               test.name AS examName,
               request.status AS status,
               item.resultValue AS resultValue,
               item.resultUnit AS resultUnit,
               item.referenceRange AS referenceRange,
               test.referenceValues AS testReferenceValues,
               COALESCE(requestedBy.fullName, '') AS doctorName,
               request.createdAt AS requestedAt,
               item.resultedAt AS resultAt
        FROM LabRequest request
        JOIN request.items item
        JOIN item.labTest test
        LEFT JOIN request.requestedBy requestedBy
        WHERE request.patient.id = :patientId
          AND request.status = :status
          AND item.resultValue IS NOT NULL
        ORDER BY request.completedAt DESC
      """)
  Page<PatientLabResultProjection> findPatientResults(
      @Param("patientId") UUID patientId, @Param("status") RequestStatus status, Pageable pageable);

  long countByPatientIdAndStatusIn(UUID patientId, java.util.Collection<RequestStatus> statuses);

  @Query(
      """
        SELECT r FROM LabRequest r
        WHERE r.hospital.id = :hospitalId
        AND (:patientId IS NULL OR r.patient.id = :patientId)
        AND (:status    IS NULL OR r.status      = :status)
        ORDER BY r.createdAt DESC
    """)
  Page<LabRequest> findWithFilters(
      @Param("hospitalId") UUID hospitalId,
      @Param("patientId") UUID patientId,
      @Param("status") RequestStatus status,
      Pageable pageable);

  // ------------------------------------------------
  // Dashboard avançado
  // ------------------------------------------------

  @Query(
      """
        SELECT COUNT(r) FROM LabRequest r
        WHERE r.hospital.id = :hospitalId
        AND r.createdAt >= :since
    """)
  long countByHospitalIdAndCreatedAtAfter(
      @Param("hospitalId") UUID hospitalId, @Param("since") OffsetDateTime since);

  @Query(
      """
        SELECT COUNT(r) FROM LabRequest r
        WHERE r.hospital.id = :hospitalId
        AND r.status = :status
    """)
  long countByHospitalIdAndStatus(
      @Param("hospitalId") UUID hospitalId, @Param("status") RequestStatus status);
}
