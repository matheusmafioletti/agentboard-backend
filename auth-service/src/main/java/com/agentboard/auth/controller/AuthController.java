package com.agentboard.auth.controller;

import com.agentboard.auth.dto.LoginRequest;
import com.agentboard.auth.dto.LoginResponse;
import com.agentboard.auth.dto.RegisterRequest;
import com.agentboard.auth.dto.RegisterResponse;
import com.agentboard.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Exposes registration and login endpoints for the auth service. */
@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;

  /**
   * Creates the controller with the given auth service.
   */
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Registers a new tenant and user, returning a JWT and API key on success.
   */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  /**
   * Authenticates an existing user, returning a JWT on success.
   */
  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
