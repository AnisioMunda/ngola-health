package ao.hospitalao.application.inpatient;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class InpatientUserProvider {

  private final UserRepository userRepository;

  public User getCurrentUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilizador não autenticado.");
    }
    return userRepository
        .findByUsername(authentication.getName())
        .orElseThrow(() -> new EntityNotFoundException("Utilizador autenticado não encontrado"));
  }
}
