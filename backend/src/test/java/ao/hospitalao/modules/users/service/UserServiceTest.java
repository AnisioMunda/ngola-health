package ao.hospitalao.modules.users.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.entity.Role;
import ao.hospitalao.modules.auth.repository.RoleRepository;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.users.dto.CreateUserRequest;
import ao.hospitalao.modules.users.mapper.UserMapper;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private HospitalRepository hospitalRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private UserMapper userMapper;

  @InjectMocks private UserService userService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void hospitalAdminCannotAssignPlatformSuperAdminRole() {
    UUID roleId = UUID.randomUUID();
    CreateUserRequest request = new CreateUserRequest();
    request.setUsername("hospital.admin");
    request.setEmail("admin@example.test");
    request.setRoleIds(Set.of(roleId));
    when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
    when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
    when(roleRepository.findById(roleId))
        .thenReturn(Optional.of(Role.builder().name("SUPER_ADMIN").build()));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "hospital-admin", null, Set.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

    assertThatThrownBy(() -> userService.create(request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("platform super-administrator");

    verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }
}
