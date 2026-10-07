package ao.hospitalao.modules.triage.repository;

import ao.hospitalao.modules.triage.entity.TriageRecord;
import ao.hospitalao.modules.triage.entity.TriageRecord.TriagePriority;
import ao.hospitalao.modules.triage.entity.TriageRecord.TriageStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TriageRepository extends JpaRepository<TriageRecord, UUID> {

  // Fila de espera actual — ordenada por prioridade depois por hora de triagem
  @Query(
      """
        SELECT t FROM TriageRecord t
        LEFT JOIN FETCH t.patient
        LEFT JOIN FETCH t.triagedBy
        WHERE t.hospital.id = :hospitalId
        AND   t.status = ao.hospitalao.modules.triage.entity.TriageRecord$TriageStatus.WAITING
        ORDER BY
            CASE t.priority
                WHEN ao.hospitalao.modules.triage.entity.TriageRecord$TriagePriority.RED    THEN 1
                WHEN ao.hospitalao.modules.triage.entity.TriageRecord$TriagePriority.ORANGE THEN 2
                WHEN ao.hospitalao.modules.triage.entity.TriageRecord$TriagePriority.YELLOW THEN 3
                WHEN ao.hospitalao.modules.triage.entity.TriageRecord$TriagePriority.GREEN  THEN 4
                WHEN ao.hospitalao.modules.triage.entity.TriageRecord$TriagePriority.BLUE   THEN 5
            END,
            t.triagedAt ASC
    """)
  List<TriageRecord> findWaitingQueue(@Param("hospitalId") UUID hospitalId);

  // Todos os activos (WAITING + IN_PROGRESS)
  @Query(
      """
        SELECT t FROM TriageRecord t
        LEFT JOIN FETCH t.patient
        LEFT JOIN FETCH t.triagedBy
        WHERE t.hospital.id = :hospitalId
        AND   t.status IN (
            ao.hospitalao.modules.triage.entity.TriageRecord$TriageStatus.WAITING,
            ao.hospitalao.modules.triage.entity.TriageRecord$TriageStatus.IN_PROGRESS
        )
        ORDER BY
            CASE t.priority
                WHEN ao.hospitalao.modules.triage.entity.TriageRecord$TriagePriority.RED    THEN 1
                WHEN ao.hospitalao.modules.triage.entity.TriageRecord$TriagePriority.ORANGE THEN 2
                WHEN ao.hospitalao.modules.triage.entity.TriageRecord$TriagePriority.YELLOW THEN 3
                WHEN ao.hospitalao.modules.triage.entity.TriageRecord$TriagePriority.GREEN  THEN 4
                WHEN ao.hospitalao.modules.triage.entity.TriageRecord$TriagePriority.BLUE   THEN 5
            END,
            t.triagedAt ASC
    """)
  List<TriageRecord> findActiveQueue(@Param("hospitalId") UUID hospitalId);

  // Histórico do dia
  @Query(
      """
        SELECT t FROM TriageRecord t
        LEFT JOIN FETCH t.patient
        WHERE t.hospital.id = :hospitalId
        AND   t.triagedAt >= :startDate
        AND   t.triagedAt < :endDate
        ORDER BY t.triagedAt DESC
    """)
  List<TriageRecord> findByDate(
      @Param("hospitalId") UUID hospitalId,
      @Param("startDate") OffsetDateTime startDate,
      @Param("endDate") OffsetDateTime endDate);

  // Estatísticas do dia
  @Query(
      """
        SELECT COUNT(t) FROM TriageRecord t
        WHERE t.hospital.id = :hospitalId
        AND   t.status = :status
        AND   t.triagedAt >= CURRENT_DATE
    """)
  long countTodayByStatus(
      @Param("hospitalId") UUID hospitalId, @Param("status") TriageStatus status);

  @Query(
      """
        SELECT COUNT(t) FROM TriageRecord t
        WHERE t.hospital.id = :hospitalId
        AND   t.triagedAt >= CURRENT_DATE
    """)
  long countToday(@Param("hospitalId") UUID hospitalId);

  @Query(
      """
        SELECT COUNT(t) FROM TriageRecord t
        WHERE t.hospital.id = :hospitalId
        AND   t.priority = :priority
        AND   t.status = 'WAITING'
    """)
  long countWaitingByPriority(
      @Param("hospitalId") UUID hospitalId, @Param("priority") TriagePriority priority);

  // Tempo médio de espera do dia (em minutos)
  @Query(
      value =
          """
        SELECT COALESCE(AVG(
            EXTRACT(EPOCH FROM (attended_at - triaged_at)) / 60
        ), 0)
        FROM triage_records
        WHERE hospital_id = :hospitalId
        AND attended_at IS NOT NULL
        AND DATE(triaged_at) = CURRENT_DATE
    """,
      nativeQuery = true)
  double avgWaitingMinutesToday(@Param("hospitalId") UUID hospitalId);

  // Próximo número da fila
  @Query(value = "SELECT nextval('queue_number_seq')", nativeQuery = true)
  long nextQueueNumber();

  // Triagem de um paciente
  Optional<TriageRecord> findTopByPatientIdAndStatusOrderByTriagedAtDesc(
      UUID patientId, TriageStatus status);
}
