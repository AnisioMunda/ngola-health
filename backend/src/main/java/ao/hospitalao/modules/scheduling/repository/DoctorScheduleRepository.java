// ============================================================
// DoctorScheduleRepository.java
// ============================================================
package ao.hospitalao.modules.scheduling.repository;

import ao.hospitalao.modules.scheduling.entity.DoctorSchedule;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, UUID> {

  List<DoctorSchedule> findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(UUID doctorId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
        SELECT ds FROM DoctorSchedule ds
        WHERE ds.doctor.id = :doctorId
        AND ds.dayOfWeek = :dayOfWeek
        AND ds.active = true
        ORDER BY ds.startTime
    """)
  List<DoctorSchedule> findActiveSchedulesForBooking(
      @Param("doctorId") UUID doctorId, @Param("dayOfWeek") int dayOfWeek);

  List<DoctorSchedule> findByHospitalIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
      UUID hospitalId);

  @Query(
      """
        SELECT ds FROM DoctorSchedule ds
        WHERE ds.hospital.id = :hospitalId
        AND ds.active = true
        AND ds.dayOfWeek = :dayOfWeek
        ORDER BY ds.startTime
    """)
  List<DoctorSchedule> findByHospitalAndDay(
      @Param("hospitalId") UUID hospitalId, @Param("dayOfWeek") int dayOfWeek);

  boolean existsByDoctorIdAndDayOfWeekAndStartTime(
      UUID doctorId, Integer dayOfWeek, java.time.LocalTime startTime);

  @Query(
      """
        SELECT COUNT(ds) > 0 FROM DoctorSchedule ds
        WHERE ds.doctor.id = :doctorId
        AND ds.dayOfWeek = :dayOfWeek
        AND ds.active = true
        AND ds.startTime < :endTime
        AND ds.endTime > :startTime
    """)
  boolean existsOverlappingSchedule(
      @Param("doctorId") UUID doctorId,
      @Param("dayOfWeek") int dayOfWeek,
      @Param("startTime") java.time.LocalTime startTime,
      @Param("endTime") java.time.LocalTime endTime);
}
