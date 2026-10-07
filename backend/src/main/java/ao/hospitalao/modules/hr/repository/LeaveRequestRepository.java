// ============================================================
// LeaveRequestRepository.java
// ============================================================
package ao.hospitalao.modules.hr.repository;
 
import ao.hospitalao.modules.hr.entity.LeaveRequest;
import ao.hospitalao.modules.hr.entity.LeaveRequest.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
 
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
 
    // Pedidos de um utilizador
    Page<LeaveRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
 
    // Pedidos pendentes do hospital (para aprovação)
    @Query("""
        SELECT l FROM LeaveRequest l
        LEFT JOIN FETCH l.user
        WHERE l.hospital.id = :hospitalId
        AND   l.status      = :status
        ORDER BY l.createdAt ASC
    """)
    List<LeaveRequest> findByHospitalAndStatus(
        @Param("hospitalId") UUID hospitalId,
        @Param("status")     LeaveStatus status
    );
 
    // Ausências aprovadas num período (para mapa de ausências)
    @Query("""
        SELECT l FROM LeaveRequest l
        LEFT JOIN FETCH l.user
        WHERE l.hospital.id = :hospitalId
        AND   l.status      = 'APPROVED'
        AND   l.startDate  <= :to
        AND   l.endDate    >= :from
        ORDER BY l.startDate
    """)
    List<LeaveRequest> findApprovedInPeriod(
        @Param("hospitalId") UUID hospitalId,
        @Param("from")       LocalDate from,
        @Param("to")         LocalDate to
    );
 
    // Verificar sobreposição de pedidos do mesmo utilizador
    @Query("""
        SELECT COUNT(l) > 0 FROM LeaveRequest l
        WHERE l.user.id    = :userId
        AND   l.status    != 'REJECTED'
        AND   l.status    != 'CANCELLED'
        AND   l.startDate <= :endDate
        AND   l.endDate   >= :startDate
        AND   l.id        != :excludeId
    """)
    boolean hasOverlap(
        @Param("userId")    UUID userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate")   LocalDate endDate,
        @Param("excludeId") UUID excludeId
    );
 
    // Total de dias usados por tipo no ano
    @Query("""
        SELECT COALESCE(SUM(l.totalDays), 0) FROM LeaveRequest l
        WHERE l.user.id    = :userId
        AND   l.leaveType  = :leaveType
        AND   l.status     = 'APPROVED'
        AND   YEAR(l.startDate) = :year
    """)
    int sumDaysUsedByType(
        @Param("userId")    UUID userId,
        @Param("leaveType") LeaveRequest.LeaveType leaveType,
        @Param("year")      int year
    );
}