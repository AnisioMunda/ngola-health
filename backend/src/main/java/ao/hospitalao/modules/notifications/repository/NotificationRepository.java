package ao.hospitalao.modules.notifications.repository;

import ao.hospitalao.modules.notifications.entity.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  @Query(
      """
        SELECT n FROM Notification n
        WHERE n.id = :id
        AND n.hospital.id = :hospitalId
        AND (n.user.id = :userId OR n.user IS NULL)
    """)
  Optional<Notification> findVisibleToUser(
      @Param("id") UUID id, @Param("hospitalId") UUID hospitalId, @Param("userId") UUID userId);

  /** Notificações do utilizador + notificações globais do hospital */
  @Query(
      """
        SELECT n FROM Notification n
        WHERE n.hospital.id = :hospitalId
        AND (n.user.id = :userId OR n.user IS NULL)
        ORDER BY n.createdAt DESC
    """)
  Page<Notification> findForUser(
      @Param("hospitalId") UUID hospitalId, @Param("userId") UUID userId, Pageable pageable);

  /** Apenas não lidas — para o contador no sino */
  @Query(
      """
        SELECT COUNT(n) FROM Notification n
        WHERE n.hospital.id = :hospitalId
        AND (n.user.id = :userId OR n.user IS NULL)
        AND n.read = false
    """)
  long countUnread(@Param("hospitalId") UUID hospitalId, @Param("userId") UUID userId);

  /** Últimas 5 não lidas — para o dropdown do sino */
  @Query(
      """
        SELECT n FROM Notification n
        WHERE n.hospital.id = :hospitalId
        AND (n.user.id = :userId OR n.user IS NULL)
        AND n.read = false
        ORDER BY n.createdAt DESC
    """)
  List<Notification> findTopUnread(
      @Param("hospitalId") UUID hospitalId, @Param("userId") UUID userId, Pageable pageable);

  /** Marcar todas como lidas */
  @Modifying
  @Query(
      """
        UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP
        WHERE n.hospital.id = :hospitalId
        AND (n.user.id = :userId OR n.user IS NULL)
        AND n.read = false
    """)
  int markAllAsRead(@Param("hospitalId") UUID hospitalId, @Param("userId") UUID userId);

  /** Evitar duplicados — verificar se já existe notificação recente do mesmo tipo e referência */
  @Query(
      """
        SELECT COUNT(n) > 0 FROM Notification n
        WHERE n.hospital.id = :hospitalId
        AND n.type = :type
        AND n.referenceId = :referenceId
        AND n.createdAt >= :since
    """)
  boolean existsRecentNotification(
      @Param("hospitalId") UUID hospitalId,
      @Param("type") Notification.NotificationType type,
      @Param("referenceId") UUID referenceId,
      @Param("since") java.time.OffsetDateTime since);
}
