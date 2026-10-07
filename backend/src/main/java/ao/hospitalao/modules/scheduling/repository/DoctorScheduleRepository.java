// ============================================================
// DoctorScheduleRepository.java
// ============================================================
package ao.hospitalao.modules.scheduling.repository;
 
import ao.hospitalao.modules.scheduling.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.UUID;
 
@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, UUID> {
 
    List<DoctorSchedule> findByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
        UUID doctorId);
 
    List<DoctorSchedule> findByHospitalIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
        UUID hospitalId);
 
    @Query("""
        SELECT ds FROM DoctorSchedule ds
        WHERE ds.hospital.id = :hospitalId
        AND ds.active = true
        AND ds.dayOfWeek = :dayOfWeek
        ORDER BY ds.startTime
    """)
    List<DoctorSchedule> findByHospitalAndDay(
        @Param("hospitalId") UUID hospitalId,
        @Param("dayOfWeek") int dayOfWeek
    );
 
    boolean existsByDoctorIdAndDayOfWeekAndStartTime(
        UUID doctorId, Integer dayOfWeek, java.time.LocalTime startTime);
}