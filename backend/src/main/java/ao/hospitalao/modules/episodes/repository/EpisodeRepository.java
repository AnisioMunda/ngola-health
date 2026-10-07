package ao.hospitalao.modules.episodes.repository;

import ao.hospitalao.modules.episodes.entity.Episode;
import ao.hospitalao.modules.episodes.entity.Episode.EpisodeStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, UUID> {

  Page<Episode> findByPatientId(UUID patientId, Pageable pageable);

  Page<Episode> findByDoctorId(UUID doctorId, Pageable pageable);

  Page<Episode> findByStatus(EpisodeStatus status, Pageable pageable);

  List<Episode> findByPatientIdOrderByCreatedAtDesc(UUID patientId, Pageable pageable);

  // Contar episódios por paciente
  long countByPatientId(UUID patientId);

  @Query(
      """
        SELECT e FROM Episode e
        WHERE (:patientId IS NULL OR e.patient.id = :patientId)
        AND   (:doctorId  IS NULL OR e.doctor.id  = :doctorId)
        AND   (:status    IS NULL OR e.status      = :status)
        ORDER BY e.scheduledAt DESC NULLS LAST, e.createdAt DESC
    """)
  Page<Episode> findWithFilters(
      @Param("patientId") UUID patientId,
      @Param("doctorId") UUID doctorId,
      @Param("status") EpisodeStatus status,
      Pageable pageable);

  // ------------------------------------------------
  // Dashboard avançado
  // ------------------------------------------------

  long countByStatus(EpisodeStatus status);

  @Query(
      """
        SELECT COUNT(e) FROM Episode e
        WHERE e.hospital.id = :hospitalId
        AND e.status = 'SCHEDULED'
        AND FUNCTION('DATE', e.scheduledAt) = :today
    """)
  long countScheduledToday(@Param("hospitalId") UUID hospitalId, @Param("today") LocalDate today);

  @Query(
      """
        SELECT COUNT(e) FROM Episode e
        WHERE e.hospital.id = :hospitalId
        AND e.status = 'COMPLETED'
        AND FUNCTION('DATE', e.completedAt) = :today
    """)
  long countCompletedToday(@Param("hospitalId") UUID hospitalId, @Param("today") LocalDate today);

  @Query(
      """
        SELECT e.episodeType AS type, COUNT(e) AS count
        FROM Episode e
        WHERE e.hospital.id = :hospitalId
        GROUP BY e.episodeType
        ORDER BY COUNT(e) DESC
    """)
  List<EpisodeTypeProjection> countByType(@Param("hospitalId") UUID hospitalId);

  @Query(
      value =
          """
        SELECT
            TO_CHAR(DATE_TRUNC('month', e.scheduled_at), 'YYYY-MM') AS yearMonth,
            COUNT(CASE WHEN e.status = 'SCHEDULED'  THEN 1 END)    AS scheduled,
            COUNT(CASE WHEN e.status = 'COMPLETED'  THEN 1 END)    AS completed,
            COUNT(CASE WHEN e.status = 'CANCELLED'  THEN 1 END)    AS cancelled
        FROM episodes e
        WHERE e.hospital_id = :hospitalId
        AND e.scheduled_at >= :since
        GROUP BY DATE_TRUNC('month', e.scheduled_at)
        ORDER BY DATE_TRUNC('month', e.scheduled_at)
    """,
      nativeQuery = true)
  List<Object[]> countByMonthRaw(
      @Param("hospitalId") UUID hospitalId, @Param("since") OffsetDateTime since);

  interface EpisodeTypeProjection {
    String getType();

    Long getCount();
  }

  // Contar episódios hoje
  @Query(
      """
        SELECT COUNT(e) FROM Episode e
        WHERE e.hospital.id = :hospitalId
        AND   CAST(e.createdAt AS date) = :date
    """)
  long countByHospitalAndDate(@Param("hospitalId") UUID hospitalId, @Param("date") LocalDate date);

  // Contar episódios num período
  @Query(
      """
        SELECT COUNT(e) FROM Episode e
        WHERE e.hospital.id = :hospitalId
        AND   CAST(e.createdAt AS date) >= :from
        AND   CAST(e.createdAt AS date) <= :to
    """)
  long countByHospitalAndPeriod(
      @Param("hospitalId") UUID hospitalId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  // Contar episódios por médico num período
  @Query(
      """
        SELECT COUNT(e) FROM Episode e
        WHERE e.doctor.id   = :doctorId
        AND   CAST(e.createdAt AS date) >= :from
        AND   CAST(e.createdAt AS date) <= :to
    """)
  long countByDoctorAndPeriod(
      @Param("doctorId") UUID doctorId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
