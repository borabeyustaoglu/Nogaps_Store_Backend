package org.example.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.business.service.AuthService;

import org.example.common.dto.auth.LoginRequest;
import org.example.common.dto.auth.LoginResult;
import org.example.common.dto.auth.LoginResponse;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.auth.RegisterRequest;
import org.example.common.security.AuthCookieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "auth-controller")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @PostMapping("/login")
    @Operation(summary = "login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult loginResult = authService.login(request);
        authCookieService.attachAuthCookie(response, loginResult.getToken(), authService.getTokenExpirationSeconds());
        return ResponseEntity.ok(loginResult.getResponse());
    }

    @PostMapping("/register")
    @Operation(summary = "register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = authCookieService.resolveToken(request);
        MessageResponse logoutResponse = authService.logout(token);
        authCookieService.clearAuthCookie(response);
        return ResponseEntity.ok(logoutResponse);
    }
}
