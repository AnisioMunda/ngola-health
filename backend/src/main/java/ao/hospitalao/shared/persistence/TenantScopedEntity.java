package ao.hospitalao.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

@MappedSuperclass
@Getter
@Setter
public abstract class TenantScopedEntity {

  @TenantId
  @Column(name = "hospital_id", updatable = false)
  private UUID hospitalId;
}
