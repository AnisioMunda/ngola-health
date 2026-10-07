package ao.hospitalao.modules.auth.repository;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.entity.enums.RegisterStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  long countByRegisterStatus(RegisterStatus status);

  // ------------------------------------------------
  // NOVO — para o HrService (Sprint 10)
  // ------------------------------------------------

  /** Total de funcionários activos do hospital */
  @Query(
      """
        SELECT COUNT(u) FROM User u
        WHERE u.hospital.id     = :hospitalId
        AND   u.registerStatus  = 'ACTIVE'
    """)
  long countByHospitalIdAndActiveTrue(@Param("hospitalId") UUID hospitalId);

  /** Listar utilizadores activos do hospital (para selectores RH) */
  @Query(
      """
        SELECT u FROM User u
        WHERE u.hospital.id    = :hospitalId
        AND   u.registerStatus = 'ACTIVE'
        ORDER BY u.fullName
    """)
  Page<User> findByHospitalIdAndActiveTrue(@Param("hospitalId") UUID hospitalId, Pageable pageable);

  // ------------------------------------------------
  // Dashboard statistics — users grouped by role
  // ------------------------------------------------
  @Query(
      """
        SELECT r.name AS roleName, COUNT(DISTINCT u) AS count
        FROM User u
        JOIN u.roles r
        WHERE u.registerStatus = 'ACTIVE'
        GROUP BY r.name
        ORDER BY COUNT(DISTINCT u) DESC
    """)
  List<RoleCountProjection> countByRole();

  interface RoleCountProjection {
    String getRoleName();

    Long getCount();
  }
}
