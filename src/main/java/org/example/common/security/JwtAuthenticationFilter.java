package org.example.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.exception.ErrorCode;
import org.example.data.repository.AuthTokenRepository;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/api/auth/",
            "/v3/api-docs",
            "/swagger-ui/"
    );
    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
            "/swagger-ui.html",
            "/swagger-ui"
    );

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AuthTokenRepository authTokenRepository;
    private final AuthCookieService authCookieService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            return false;
        }

        if (PUBLIC_EXACT_PATHS.contains(path)) {
            return true;
        }

        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = authCookieService.resolveToken(request);
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(token);
            String jti = jwtService.extractJti(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                boolean activeToken = authTokenRepository.existsByJtiAndRevokedFalseAndExpiresAtAfter(
                        jti, LocalDateTime.now()
                );
                if (!activeToken || jwtService.isTokenExpired(token)) {
                    writeUnauthorizedResponse(response);
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                refreshTokenIfHalfLifeReached(token, jti, (AppUserDetails) userDetails, response);
            }
            filterChain.doFilter(request, response);
        } catch (JwtException ex) {
            writeUnauthorizedResponse(response);
        }
    }

    private void refreshTokenIfHalfLifeReached(String token, String jti, AppUserDetails userDetails, HttpServletResponse response) {
        Date issuedAt = jwtService.extractIssuedAt(token);
        Date expiration = jwtService.extractExpiration(token);
        if (issuedAt == null || expiration == null) {
            return;
        }
        long totalLifetimeMillis = expiration.getTime() - issuedAt.getTime();
        if (totalLifetimeMillis <= 0) {
            return;
        }
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();

        if (remainingMillis > (totalLifetimeMillis / 2)) {
            return;
        }

        long refreshLifetimeMillis = totalLifetimeMillis;
        String refreshedToken = jwtService.generateToken(userDetails, refreshLifetimeMillis, jti);
        long refreshLifetimeSeconds = refreshLifetimeMillis / 1000;
        authCookieService.attachAuthCookie(response, refreshedToken, refreshLifetimeSeconds);

        authTokenRepository.findByJti(jti).ifPresent(authToken -> {
            authToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshLifetimeSeconds));
            authTokenRepository.save(authToken);
        });
    }

    private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new MessageResponse(ErrorCode.TOKEN_INVALID_OR_EXPIRED.getMessage()));
    }
}
