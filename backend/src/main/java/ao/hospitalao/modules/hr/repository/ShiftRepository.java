// ============================================================
// ShiftRepository.java
// ============================================================
package ao.hospitalao.modules.hr.repository;
 
import ao.hospitalao.modules.hr.entity.Shift;
import ao.hospitalao.modules.hr.entity.Shift.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
 
@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID> {
 
    // Turnos de um utilizador num período
    @Query("""
        SELECT s FROM Shift s
        LEFT JOIN FETCH s.user
        LEFT JOIN FETCH s.ward
        WHERE s.hospital.id = :hospitalId
        AND s.user.id = :userId
        AND s.shiftDate BETWEEN :from AND :to
        ORDER BY s.shiftDate, s.startTime
    """)
    List<Shift> findByUserAndPeriod(
        @Param("hospitalId") UUID hospitalId,
        @Param("userId")     UUID userId,
        @Param("from")       LocalDate from,
        @Param("to")         LocalDate to
    );
 
    // Todos os turnos do hospital num período (para escala geral)
    @Query("""
        SELECT s FROM Shift s
        LEFT JOIN FETCH s.user
        LEFT JOIN FETCH s.ward
        WHERE s.hospital.id = :hospitalId
        AND s.shiftDate BETWEEN :from AND :to
        ORDER BY s.shiftDate, s.startTime
    """)
    List<Shift> findByHospitalAndPeriod(
        @Param("hospitalId") UUID hospitalId,
        @Param("from")       LocalDate from,
        @Param("to")         LocalDate to
    );
 
    // Turnos por data e enfermaria
    List<Shift> findByHospitalIdAndShiftDateAndWardIdOrderByStartTime(
        UUID hospitalId, LocalDate shiftDate, UUID wardId);
 
    // Verificar conflito de turnos
    @Query("""
        SELECT COUNT(s) > 0 FROM Shift s
        WHERE s.user.id    = :userId
        AND   s.shiftDate  = :date
        AND   s.status NOT IN ('CANCELLED', 'SWAPPED')
        AND   s.id        != :excludeId
    """)
    boolean hasConflict(
        @Param("userId")    UUID userId,
        @Param("date")      LocalDate date,
        @Param("excludeId") UUID excludeId
    );
}