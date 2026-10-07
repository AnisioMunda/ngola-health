package ao.hospitalao.modules.portal.repository;

import ao.hospitalao.modules.portal.entity.PatientPortalAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientPortalAccountRepository extends JpaRepository<PatientPortalAccount, UUID> {

  /** Procura uma conta do portal pelo email. Usado no processo de login. */
  Optional<PatientPortalAccount> findByEmail(String email);

  /** Verifica se já existe uma conta do portal associada ao paciente. */
  boolean existsByPatientId(UUID patientId);

  /** Verifica se já existe uma conta com determinado email. */
  boolean existsByEmail(String email);
}
