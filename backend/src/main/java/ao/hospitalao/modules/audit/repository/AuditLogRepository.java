package ao.hospitalao.modules.audit.repository;

import ao.hospitalao.modules.audit.entity.AuditLog;
import ao.hospitalao.modules.audit.entity.AuditLog.AuditAction;
import ao.hospitalao.modules.audit.entity.AuditLog.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository
    extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    // Histórico de uma entidade específica
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.entityType = :entityType
        AND   a.entityId   = :entityId
        ORDER BY a.createdAt DESC
    """)
    List<AuditLog> findByEntity(
        @Param("entityType") EntityType entityType,
        @Param("entityId")   String entityId
    );

    // Actividade recente de um utilizador
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Estatísticas
    @Query("""
        SELECT COUNT(a) FROM AuditLog a
        WHERE a.hospital.id = :hospitalId
        AND a.createdAt >= :since
    """)
    long countByHospitalSince(
        @Param("hospitalId") UUID hospitalId,
        @Param("since")      OffsetDateTime since
    );

    @Query("""
        SELECT COUNT(a) FROM AuditLog a
        WHERE a.hospital.id = :hospitalId
        AND a.action = ao.hospitalao.modules.audit.entity.AuditLog$AuditAction.LOGIN_FAILED
        AND a.createdAt >= :since
    """)
    long countFailedLoginsSince(
        @Param("hospitalId") UUID hospitalId,
        @Param("since")      OffsetDateTime since
    );

    // Top utilizadores mais activos
    @Query(value = """
        SELECT username, user_full_name, COUNT(*) AS total
        FROM audit_logs
        WHERE hospital_id = :hospitalId
        AND created_at >= :since
        AND username IS NOT NULL
        GROUP BY username, user_full_name
        ORDER BY total DESC
        LIMIT 5
    """, nativeQuery = true)
    List<Object[]> findTopActiveUsers(
        @Param("hospitalId") UUID hospitalId,
        @Param("since")      OffsetDateTime since
    );
}