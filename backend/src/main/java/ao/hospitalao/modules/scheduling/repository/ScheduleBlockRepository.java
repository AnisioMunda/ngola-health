// ============================================================
// ScheduleBlockRepository.java
// ============================================================
package ao.hospitalao.modules.scheduling.repository;

import ao.hospitalao.modules.scheduling.entity.ScheduleBlock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, UUID> {

  List<ScheduleBlock> findByDoctorIdAndBlockDateBetweenOrderByBlockDate(
      UUID doctorId, LocalDate from, LocalDate to);

  @Query(
      """
        SELECT b FROM ScheduleBlock b
        WHERE b.doctor.id = :doctorId
        AND b.blockDate = :date
    """)
  List<ScheduleBlock> findByDoctorAndDate(
      @Param("doctorId") UUID doctorId, @Param("date") LocalDate date);
}
