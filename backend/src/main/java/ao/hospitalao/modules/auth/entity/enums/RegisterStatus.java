package ao.hospitalao.modules.auth.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RegisterStatus {
  ACTIVE("Activo"),
  INACTIVE("Inactivo"),
  SUSPENDED("Suspenso"),
  ;

  private final String descricao;
}
