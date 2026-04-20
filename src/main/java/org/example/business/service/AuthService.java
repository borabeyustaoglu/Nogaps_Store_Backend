package org.example.business.service;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.example.common.dto.auth.LoginRequest;
import org.example.common.dto.auth.LoginResponse;
import org.example.common.dto.auth.LoginResult;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.auth.RegisterRequest;
import org.example.common.dto.permission.PermissionResponse;
import org.example.common.entity.AppRole;
import org.example.common.entity.AppUser;
import org.example.common.entity.AuthToken;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.common.security.AppUserDetails;
import org.example.common.security.JwtService;
import org.example.data.repository.AppRoleRepository;
import org.example.data.repository.AppUserRepository;
import org.example.data.repository.AuthTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthTokenRepository authTokenRepository;
    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final CouponService couponService;
    private final AuditLogService auditLogService;

    @Value("${app.jwt.expiration-hours:8}")
    private long tokenExpirationHours;

    public LoginResult login(LoginRequest request) {
        passwordPolicyService.validateOrThrow(request.getPassword());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        String jti = UUID.randomUUID().toString();
        long expirationMillis = tokenExpirationHours * 60 * 60 * 1000;
        String token = jwtService.generateToken(userDetails, expirationMillis, jti);

        AuthToken authToken = new AuthToken();
        authToken.setJti(jti);
        authToken.setUser(userDetails.getUser());
        authToken.setRevoked(false);
        authToken.setExpiresAt(LocalDateTime.now().plusHours(tokenExpirationHours));
        authTokenRepository.save(authToken);

        List<PermissionResponse> permissions = userDetails.getUser().getRole().getPermissions().stream()
                .map(permission -> new PermissionResponse(permission.getCode(), permission.getDescription()))
                .sorted((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()))
                .toList();

        LoginResponse response = new LoginResponse(
                userDetails.getFullName(),
                userDetails.getEmail(),
                userDetails.getPhoneNumber(),
                userDetails.getAddress(),
                userDetails.getRoleName(),
                permissions
        );

        auditLogService.logAs(
                userDetails.getUsername(),
                userDetails.getFullName(),
                userDetails.getRoleName(),
                "LOGIN",
                "AUTH",
                userDetails.getId(),
                "User logged in successfully."
        );

        return new LoginResult(token, response);
    }

    public MessageResponse register(RegisterRequest request) {
        passwordPolicyService.validateOrThrow(request.getPassword());

        if (appUserRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        AppRole userRole = appRoleRepository.findByName("USER")
                .orElseThrow(() -> new AppException(ErrorCode.USER_ROLE_NOT_FOUND));

        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setRole(userRole);
        appUserRepository.save(user);

        couponService.assignWelcomeCouponToUser(user);

        auditLogService.logAs(
                user.getUsername(),
                user.getFullName(),
                userRole.getName(),
                "REGISTER",
                "AUTH",
                user.getId(),
                "User registered."
        );

        return new MessageResponse("Kayit basarili.");
    }

    public MessageResponse logout(String token) {
        if (token == null || token.isBlank()) {
            return new MessageResponse("Cikis basarili.");
        }

        try {
            String jti = jwtService.extractJti(token);
            authTokenRepository.findByJti(jti).ifPresent(authToken -> {
                String username = authToken.getUser().getUsername();
                String fullName = authToken.getUser().getFullName();
                String role = authToken.getUser().getRole().getName();
                Integer userId = authToken.getUser().getId();
                authToken.setRevoked(true);
                authToken.setRevokedAt(LocalDateTime.now());
                authTokenRepository.save(authToken);
                auditLogService.logAs(
                        username,
                        fullName,
                        role,
                        "LOGOUT",
                        "AUTH",
                        userId,
                        "User logged out."
                );
            });
        } catch (JwtException ignored) {
            // Token invalid or already expired.
        }

        SecurityContextHolder.clearContext();
        return new MessageResponse("Cikis basarili.");
    }

    public long getTokenExpirationSeconds() {
        return tokenExpirationHours * 3600;
    }
}