// ============================================================
// AttendanceRepository.java
// ============================================================
package ao.hospitalao.modules.hr.repository;
 
import ao.hospitalao.modules.hr.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, UUID> {
 
    // Registo de ponto de um utilizador numa data
    Optional<AttendanceRecord> findByUserIdAndWorkDate(UUID userId, LocalDate workDate);
 
    // Registos de ponto de um utilizador num período
    @Query("""
        SELECT a FROM AttendanceRecord a
        LEFT JOIN FETCH a.user
        WHERE a.hospital.id = :hospitalId
        AND   a.user.id     = :userId
        AND   a.workDate BETWEEN :from AND :to
        ORDER BY a.workDate DESC
    """)
    List<AttendanceRecord> findByUserAndPeriod(
        @Param("hospitalId") UUID hospitalId,
        @Param("userId")     UUID userId,
        @Param("from")       LocalDate from,
        @Param("to")         LocalDate to
    );
 
    // Todos os registos do hospital numa data (para relatório diário)
    @Query("""
        SELECT a FROM AttendanceRecord a
        LEFT JOIN FETCH a.user
        WHERE a.hospital.id = :hospitalId
        AND   a.workDate    = :date
        ORDER BY a.user.fullName
    """)
    List<AttendanceRecord> findByHospitalAndDate(
        @Param("hospitalId") UUID hospitalId,
        @Param("date")       LocalDate date
    );
 
    // Estatísticas de presença num período
    @Query("""
        SELECT COUNT(a) FROM AttendanceRecord a
        WHERE a.hospital.id = :hospitalId
        AND   a.workDate BETWEEN :from AND :to
        AND   a.status = :status
    """)
    long countByStatus(
        @Param("hospitalId") UUID hospitalId,
        @Param("from")       LocalDate from,
        @Param("to")         LocalDate to,
        @Param("status")     AttendanceRecord.AttendanceStatus status
    );
 
    // Total de horas trabalhadas por utilizador num período
    @Query("""
        SELECT COALESCE(SUM(a.minutesWorked), 0) FROM AttendanceRecord a
        WHERE a.user.id   = :userId
        AND   a.workDate BETWEEN :from AND :to
    """)
    long sumMinutesWorked(
        @Param("userId") UUID userId,
        @Param("from")   LocalDate from,
        @Param("to")     LocalDate to
    );
}