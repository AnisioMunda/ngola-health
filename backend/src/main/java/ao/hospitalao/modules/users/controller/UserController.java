package ao.hospitalao.modules.users.controller;

import ao.hospitalao.modules.auth.entity.enums.RegisterStatus;
import ao.hospitalao.modules.users.dto.CreateUserRequest;
import ao.hospitalao.modules.users.dto.UpdateUserRequest;
import ao.hospitalao.modules.users.dto.UserResponse;
import ao.hospitalao.modules.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final UserService userService;

  @GetMapping
  @Operation(summary = "List all users (paginated)")
  @PreAuthorize(
      "hasRole(T(ao.hospitalao.security.RoleName).ADMIN.name()) or hasRole(T(ao.hospitalao.security.RoleName).MANAGER.name())")
  public ResponseEntity<Page<UserResponse>> findAll(
      @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
    return ResponseEntity.ok(userService.findAll(pageable));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get user by ID")
  @PreAuthorize(
      "hasRole(T(ao.hospitalao.security.RoleName).ADMIN.name()) or hasRole(T(ao.hospitalao.security.RoleName).MANAGER.name()) or #id == authentication.principal.id")
  public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(userService.findById(id));
  }

  @PostMapping
  @Operation(summary = "Create new user")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).ADMIN.name())")
  public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
    UserResponse created = userService.create(request);
    URI uri =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(uri).body(created);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update user")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).ADMIN.name())")
  public ResponseEntity<UserResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
    return ResponseEntity.ok(userService.update(id, request));
  }

  @PatchMapping("/{id}/activate")
  @Operation(summary = "Activate user")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).ADMIN.name())")
  public ResponseEntity<UserResponse> activate(@PathVariable UUID id) {
    return ResponseEntity.ok(userService.setStatus(id, RegisterStatus.ACTIVE));
  }

  @PatchMapping("/{id}/deactivate")
  @Operation(summary = "Deactivate user")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).ADMIN.name())")
  public ResponseEntity<UserResponse> deactivate(@PathVariable UUID id) {
    return ResponseEntity.ok(userService.setStatus(id, RegisterStatus.INACTIVE));
  }

  @PatchMapping("/{id}/suspend")
  @Operation(summary = "Suspend user")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).ADMIN.name())")
  public ResponseEntity<UserResponse> suspend(@PathVariable UUID id) {
    return ResponseEntity.ok(userService.setStatus(id, RegisterStatus.SUSPENDED));
  }

  @PatchMapping("/{id}/reset-password")
  @Operation(summary = "Reset user password (admin)")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).ADMIN.name())")
  public ResponseEntity<Void> resetPassword(
      @PathVariable UUID id, @RequestBody String newPassword) {
    userService.resetPassword(id, newPassword);
    return ResponseEntity.noContent().build();
  }
}
