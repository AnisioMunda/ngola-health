package ao.hospitalao.modules.notifications.entity;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    /** null = notificação para todos os utilizadores do hospital */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "type", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "priority", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /** URL de navegação no frontend (ex: /pharmacy/UUID) */
    @Column(name = "action_url", length = 300)
    private String actionUrl;

    /** UUID da entidade relacionada */
    @Column(name = "reference_id")
    private UUID referenceId;

    /** Tipo da entidade (INVOICE, LAB_REQUEST, MEDICATION, APPOINTMENT) */
    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = OffsetDateTime.now(); }

    public enum NotificationType {
        LOW_STOCK,
        EXPIRING_STOCK,
        LAB_RESULT,
        APPOINTMENT,
        APPOINTMENT_CANCELLED,
        INVOICE_OVERDUE,
        SYSTEM
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}