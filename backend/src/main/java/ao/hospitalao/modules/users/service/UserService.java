package ao.hospitalao.modules.users.service;

import ao.hospitalao.exceptions.EmailAlreadyExistsException;
import ao.hospitalao.modules.auth.entity.Role;
import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.entity.enums.RegisterStatus;
import ao.hospitalao.modules.auth.repository.RoleRepository;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.users.dto.CreateUserRequest;
import ao.hospitalao.modules.users.dto.UpdateUserRequest;
import ao.hospitalao.modules.users.dto.UserResponse;
import ao.hospitalao.modules.users.mapper.UserMapper;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  // ------------------------------------------------
  // List all users (paginated)
  // ------------------------------------------------
  @Transactional(readOnly = true)
  public Page<UserResponse> findAll(Pageable pageable) {
    return userRepository.findAll(pageable).map(userMapper::toResponse);
  }

  // ------------------------------------------------
  // Find by ID
  // ------------------------------------------------
  @Transactional(readOnly = true)
  public UserResponse findById(UUID id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    return userMapper.toResponse(user);
  }

  // ------------------------------------------------
  // Create user
  // ------------------------------------------------
  @Transactional
  public UserResponse create(CreateUserRequest request) {
    log.info("Creating user: {}", request.getUsername());

    if (userRepository.existsByUsername(request.getUsername())) {
      throw new IllegalArgumentException("Username already in use: " + request.getUsername());
    }
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new EmailAlreadyExistsException("Email already in use: " + request.getEmail());
    }

    Set<Role> roles = resolveRoles(request.getRoleIds());

    User user =
        User.builder()
            .fullName(request.getFullName())
            .username(request.getUsername().toLowerCase().trim())
            .email(request.getEmail().toLowerCase().trim())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .phone(request.getPhone())
            .especiality(request.getEspeciality())
            .professionalCard(request.getProfessionalCard())
            .registerStatus(RegisterStatus.ACTIVE)
            .mustChangePassword(request.isMustChangePassword())
            .roles(roles)
            .build();

    User saved = userRepository.save(user);
    log.info("User created: {} ({})", saved.getUsername(), saved.getId());
    return userMapper.toResponse(saved);
  }

  // ------------------------------------------------
  // Update user
  // ------------------------------------------------
  @Transactional
  public UserResponse update(UUID id, UpdateUserRequest request) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

    if (request.getFullName() != null) {
      user.setFullName(request.getFullName());
    }
    if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
      if (userRepository.existsByEmail(request.getEmail())) {
        throw new EmailAlreadyExistsException("Email already in use: " + request.getEmail());
      }
      user.setEmail(request.getEmail().toLowerCase().trim());
    }
    if (request.getPhone() != null) {
      user.setPhone(request.getPhone());
    }
    if (request.getEspeciality() != null) {
      user.setEspeciality(request.getEspeciality());
    }
    if (request.getProfessionalCard() != null) {
      user.setProfessionalCard(request.getProfessionalCard());
    }
    if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
      user.setRoles(resolveRoles(request.getRoleIds()));
    }

    User saved = userRepository.save(user);
    log.info("User updated: {}", saved.getId());
    return userMapper.toResponse(saved);
  }

  // ------------------------------------------------
  // Activate / Deactivate
  // ------------------------------------------------
  @Transactional
  public UserResponse setStatus(UUID id, RegisterStatus status) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

    user.setRegisterStatus(status);
    User saved = userRepository.save(user);
    log.info("User {} status changed to {}", id, status);
    return userMapper.toResponse(saved);
  }

  // ------------------------------------------------
  // Reset password (admin)
  // ------------------------------------------------
  @Transactional
  public void resetPassword(UUID id, String newPassword) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.setMustChangePassword(true);
    userRepository.save(user);
    log.info("Password reset for user: {}", id);
  }

  // ------------------------------------------------
  // Helper
  // ------------------------------------------------
  private Set<Role> resolveRoles(Set<UUID> roleIds) {
    Set<Role> roles = new HashSet<>();
    for (UUID roleId : roleIds) {
      Role role =
          roleRepository
              .findById(roleId)
              .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));
      if (SUPER_ADMIN_ROLE.equals(role.getName()) && !canManagePlatformRoles()) {
        throw new AccessDeniedException("Only a platform super-administrator may assign this role");
      }
      roles.add(role);
    }
    return roles;
  }

  private boolean canManagePlatformRoles() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
  }
}
