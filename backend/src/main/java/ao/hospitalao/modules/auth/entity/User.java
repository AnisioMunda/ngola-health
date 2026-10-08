package ao.hospitalao.modules.auth.entity;

import ao.hospitalao.modules.auth.entity.enums.RegisterStatus;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends TenantScopedEntity implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "full_name", nullable = false, length = 200)
  private String fullName;

  @Column(name = "username", nullable = false, unique = true, length = 100)
  private String username;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "email", unique = true, length = 200)
  private String email;

  @Column(name = "phone", length = 20)
  private String phone;

  @Column(name = "especiality", length = 100)
  private String especiality;

  @Column(name = "professional_card_number", length = 50)
  private String professionalCard;

  @Column(name = "register_status", nullable = false)
  @Enumerated(EnumType.STRING)
  private RegisterStatus registerStatus;

  @Column(name = "must_change_password", nullable = false)
  private boolean mustChangePassword;

  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts;

  @Column(name = "locked_until")
  private OffsetDateTime lockedUntil;

  @Column(name = "last_login")
  private OffsetDateTime lastLogin;

  // Multi-tenancy — hospital a que o utilizador pertence
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "hospital_id", insertable = false, updatable = false)
  private Hospital hospital;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  @Builder.Default
  private Set<Role> roles = new HashSet<>();

  // ------------------------------------------------
  // Spring Security — UserDetails
  // ------------------------------------------------

  @Override
  public String getPassword() {
    return this.passwordHash;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Set<GrantedAuthority> authorities = new HashSet<>();
    for (Role role : roles) {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
      for (Permission permission : role.getPermissions()) {
        authorities.add(new SimpleGrantedAuthority(permission.getName()));
      }
    }
    return authorities;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return this.registerStatus != RegisterStatus.SUSPENDED
        && (this.lockedUntil == null || !this.lockedUntil.isAfter(OffsetDateTime.now()));
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return this.registerStatus == RegisterStatus.ACTIVE;
  }

  // ------------------------------------------------
  // JPA lifecycle
  // ------------------------------------------------

  @PrePersist
  protected void onCreate() {
    this.createdAt = OffsetDateTime.now();
    this.updatedAt = OffsetDateTime.now();
    if (this.registerStatus == null) this.registerStatus = RegisterStatus.ACTIVE;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = OffsetDateTime.now();
  }
}
