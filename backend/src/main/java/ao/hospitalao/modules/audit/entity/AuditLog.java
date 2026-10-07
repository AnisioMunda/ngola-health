package ao.hospitalao.modules.audit.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id")
  private Hospital hospital;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "username", length = 100)
  private String username;

  @Column(name = "user_full_name", length = 200)
  private String userFullName;

  @Column(name = "action", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private AuditAction action;

  @Column(name = "entity_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private EntityType entityType;

  @Column(name = "entity_id", length = 100)
  private String entityId;

  @Column(name = "description", nullable = false, length = 500)
  private String description;

  @Column(name = "old_values", columnDefinition = "TEXT")
  private String oldValues;

  @Column(name = "new_values", columnDefinition = "TEXT")
  private String newValues;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "user_agent", length = 500)
  private String userAgent;

  @Column(name = "http_method", length = 10)
  private String httpMethod;

  @Column(name = "request_url", length = 500)
  private String requestUrl;

  @Column(name = "result", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private AuditResult result = AuditResult.SUCCESS;

  @Column(name = "error_message", length = 500)
  private String errorMessage;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
  }

  public enum AuditAction {
    CREATE,
    READ,
    UPDATE,
    DELETE,
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,
    EXPORT,
    PRINT,
    APPROVE,
    REJECT
  }

  public enum EntityType {
    PATIENT,
    EPISODE,
    LAB_REQUEST,
    MEDICATION,
    INVOICE,
    APPOINTMENT,
    ADMISSION,
    USER,
    NOTIFICATION,
    REPORT,
    WARD,
    BED,
    SYSTEM
  }

  public enum AuditResult {
    SUCCESS,
    FAILURE,
    UNAUTHORIZED
  }
}
