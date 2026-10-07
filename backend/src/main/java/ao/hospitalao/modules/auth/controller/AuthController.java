package ao.hospitalao.modules.auth.controller;

import ao.hospitalao.modules.auth.dto.AuthRequest;
import ao.hospitalao.modules.auth.dto.AuthResponse;
import ao.hospitalao.modules.auth.dto.LogoutRequest;
import ao.hospitalao.modules.auth.dto.RefreshTokenRequest;
import ao.hospitalao.modules.auth.dto.RegisterRequest;
import ao.hospitalao.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication controller")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @Operation(summary = "Register new user", description = "Create a new user account")
  @PreAuthorize("hasRole(T(ao.hospitalao.security.RoleName).ADMIN.name())")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Succes created a user",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Email is already in use")
      })
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);

    URI uri =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/users/{id}")
            .buildAndExpand(response.getId())
            .toUri();

    return ResponseEntity.created(uri).body(response);
  }

  @PostMapping("/login")
  @Operation(
      summary = "Authenticate User",
      description = "Confirm user credentials and return an authentication token")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
      })
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
    return ResponseEntity.ok(authService.authenticate(request));
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Refresh JWT",
      description = "Generate a new JWT using a valid refresh token")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token")
      })
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(authService.refreshToken("Bearer " + request.getRefreshToken()));
  }

  @PostMapping("/logout")
  @Operation(
      summary = "Logout User",
      description = "Revoga o refresh token e, quando válido, o token de acesso associado")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Logout successful"),
        @ApiResponse(responseCode = "400", description = "Refresh token ausente ou malformado"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token")
      })
  public ResponseEntity<Void> logout(
      @Valid @RequestBody LogoutRequest logoutRequest, HttpServletRequest request) {
    authService.logout(request.getHeader("Authorization"), logoutRequest.getRefreshToken());
    return ResponseEntity.noContent().build();
  }
}
