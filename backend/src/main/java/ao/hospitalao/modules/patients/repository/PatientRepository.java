package ao.hospitalao.modules.patients.repository;

import ao.hospitalao.modules.patients.dto.PatientDuplicateCandidateResponse;
import ao.hospitalao.modules.patients.entity.Patient;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

  @Query(
      """
        SELECT p FROM Patient p
        WHERE p.hospitalId = :hospitalId
        AND p.active = true
        AND (
            LOWER(p.fullName)     LIKE LOWER(CONCAT('%', :search, '%'))
            OR p.nationalId       LIKE CONCAT('%', :search, '%')
            OR p.healthCardNumber LIKE CONCAT('%', :search, '%')
            OR p.phone             LIKE CONCAT('%', :search, '%')
        )
    """)
  Page<Patient> search(
      @Param("hospitalId") UUID hospitalId, @Param("search") String search, Pageable pageable);

  Page<Patient> findByHospitalIdAndActiveTrue(UUID hospitalId, Pageable pageable);

  Optional<Patient> findByHospitalIdAndId(UUID hospitalId, UUID id);

  boolean existsByHospitalIdAndNationalId(UUID hospitalId, String nationalId);

  boolean existsByHospitalIdAndHealthCardNumber(UUID hospitalId, String healthCardNumber);

  boolean existsByHospitalIdAndNationalIdAndIdNot(UUID hospitalId, String nationalId, UUID id);

  boolean existsByHospitalIdAndHealthCardNumberAndIdNot(
      UUID hospitalId, String healthCardNumber, UUID id);

  @Query(
      """
        SELECT new ao.hospitalao.modules.patients.dto.PatientDuplicateCandidateResponse(
            p.id, p.fullName, p.birthDate)
        FROM Patient p
        WHERE p.hospitalId = :hospitalId
        AND p.active = true
        AND p.birthDate = :birthDate
        AND (
            LOWER(TRIM(p.fullName)) = LOWER(TRIM(:fullName))
            OR (:phone IS NOT NULL AND p.phone = :phone)
        )
        ORDER BY p.fullName
    """)
  List<PatientDuplicateCandidateResponse> findPossibleDuplicates(
      @Param("hospitalId") UUID hospitalId,
      @Param("fullName") String fullName,
      @Param("birthDate") LocalDate birthDate,
      @Param("phone") String phone,
      Pageable pageable);

  // ------------------------------------------------
  // Dashboard básico
  // ------------------------------------------------

  long countByActiveTrue();

  long countByActiveTrueAndGender(Patient.Gender gender);

  long countByActiveTrueAndCreatedAtAfter(OffsetDateTime since);

  @Query(
      """
        SELECT p.province AS province, COUNT(p) AS count
        FROM Patient p
        WHERE p.active = true AND p.province IS NOT NULL
        GROUP BY p.province
        ORDER BY COUNT(p) DESC
    """)
  List<ProvinceCountProjection> countByProvince(Pageable pageable);

  List<Patient> findTop5ByActiveTrueOrderByCreatedAtDesc();

  // ------------------------------------------------
  // Dashboard avançado — tendência mensal
  // ------------------------------------------------

  @Query(
      value =
          """
        SELECT
            TO_CHAR(DATE_TRUNC('month', p.created_at), 'YYYY-MM') AS yearMonth,
            COUNT(p.id)                                             AS newPatients,
            COUNT(DISTINCT e.id)                                    AS totalEpisodes
        FROM patients p
        LEFT JOIN episodes e ON e.patient_id = p.id
        WHERE p.hospital_id = :hospitalId
        AND p.created_at >= :since
        GROUP BY DATE_TRUNC('month', p.created_at)
        ORDER BY DATE_TRUNC('month', p.created_at)
    """,
      nativeQuery = true)
  List<Object[]> countByMonthRaw(
      @Param("hospitalId") UUID hospitalId, @Param("since") OffsetDateTime since);

  interface ProvinceCountProjection {
    String getProvince();

    Long getCount();
  }

  @Query(
      """
        SELECT COUNT(p) FROM Patient p
        WHERE p.hospitalId = :hospitalId
        AND   p.active = true
    """)
  long countByHospitalId(@Param("hospitalId") UUID hospitalId);

  // Contar novos pacientes num período
  @Query(
      """
        SELECT COUNT(p) FROM Patient p
        WHERE p.hospitalId = :hospitalId
        AND   p.createdAt >= :from
        AND   p.createdAt < :to
    """)
  long countByHospitalAndPeriod(
      @Param("hospitalId") UUID hospitalId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);

  @Query(
      """
        SELECT p
        FROM Patient p
        WHERE p.active = true
        AND (p.nationalId = :value OR p.healthCardNumber = :value)
    """)
  List<Patient> findAllByPortalIdentifier(@Param("value") String value);
}
