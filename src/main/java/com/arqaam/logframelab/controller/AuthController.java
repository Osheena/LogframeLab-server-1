package com.arqaam.logframelab.controller;

import com.arqaam.logframelab.controller.dto.auth.create.UserAuthProvisioningRequestDto;
import com.arqaam.logframelab.controller.dto.auth.create.UserAuthProvisioningResponseDto;
import com.arqaam.logframelab.controller.dto.auth.login.AuthenticateUserRequestDto;
import com.arqaam.logframelab.controller.dto.auth.login.JwtAuthenticationTokenResponse;
import com.arqaam.logframelab.controller.dto.auth.logout.LogoutUserRequestDto;
import com.arqaam.logframelab.controller.dto.auth.logout.LogoutUserResponseDto;
import com.arqaam.logframelab.exception.UnauthorizedException;
import com.arqaam.logframelab.model.persistence.auth.User;
import com.arqaam.logframelab.service.auth.AuthService;
import com.arqaam.logframelab.service.usermanager.UserManager;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    value = "auth",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

  private final AuthService authService;
  private final UserManager userManager;

  public AuthController(AuthService authService, UserManager userManager) {
    this.authService = authService;
    this.userManager = userManager;
  }

  @PostMapping(value = "login")
  @PreAuthorize("isAnonymous()")
  public ResponseEntity<JwtAuthenticationTokenResponse> authenticateUser(
      @Valid @RequestBody AuthenticateUserRequestDto authenticateUserRequest) {
    Authentication authentication =
        authService
            .authenticateUser(authenticateUserRequest.getUsername(),
                authenticateUserRequest.getPassword())
            .orElseThrow(() -> new UnauthorizedException("Unable to authenticate user"));

    User user = (User) authentication.getPrincipal();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    String token = authService.generateToken(user);
    return ResponseEntity.ok(new JwtAuthenticationTokenResponse(token, authService.getTokenType(),
        authService.getTokenExpiryInMillis()));
  }

  @PostMapping("logout")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<LogoutUserResponseDto> userLogout(
      @Valid @RequestBody LogoutUserRequestDto logoutUserRequest) {
    authService.logout(logoutUserRequest.getUsername());
    return ResponseEntity.ok(new LogoutUserResponseDto(true));
  }


  @PostMapping("user")
  @PreAuthorize("hasAnyAuthority('CRUD_ADMIN', 'CRUD_APP_USER')")
  public ResponseEntity<UserAuthProvisioningResponseDto> provisionUser(
      @Valid @RequestBody UserAuthProvisioningRequestDto authProvisioningRequest) {
    User user = userManager.provisionUser(authProvisioningRequest);

    return ResponseEntity.ok(new UserAuthProvisioningResponseDto(user.getUsername(),
        user.getGroupMembership().stream().map(m -> m.getGroup().getName())
            .collect(Collectors.toSet())));
  }
}
