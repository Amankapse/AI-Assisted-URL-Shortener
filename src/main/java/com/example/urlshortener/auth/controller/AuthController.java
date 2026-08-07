package com.example.urlshortener.auth.controller;

import com.example.urlshortener.auth.config.AuthProperties;
import com.example.urlshortener.auth.dto.AuthDtos.AuthResponse;
import com.example.urlshortener.auth.dto.AuthDtos.LoginRequest;
import com.example.urlshortener.auth.dto.AuthDtos.RegisterRequest;
import com.example.urlshortener.auth.dto.AuthDtos.UserResponse;
import com.example.urlshortener.auth.service.AuthService;
import com.example.urlshortener.common.metrics.AppMetrics;
import com.example.urlshortener.common.ratelimit.RateLimiterService;
import com.example.urlshortener.common.web.ClientIpResolver;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.security.JwtPrincipal;
import com.example.urlshortener.user.entity.UserStatus;
import com.example.urlshortener.user.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.net.URI;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuthProperties properties;
    private final RateLimiterService rateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final AppMetrics metrics;

    public AuthController(AuthService authService,
                          UserRepository userRepository,
                          AuthProperties properties,
                          RateLimiterService rateLimiter,
                          ClientIpResolver clientIpResolver,
                          AppMetrics metrics) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
        this.metrics = metrics;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        rateLimiter.enforce("registration", clientIpResolver.resolve(servletRequest));
        try {
            UserResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException ex) {
            metrics.auth("registration", "failure");
            throw ex;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String accountDigest = rateLimiter.digest(normalizeForLimiter(request.email()));
        rateLimiter.enforce("login", clientIpResolver.resolve(servletRequest) + ":" + accountDigest);
        try {
            AuthService.LoginResult result = authService.login(request);
            metrics.auth("login", "success");
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie(result.rawRefreshToken()).toString())
                    .body(result.response());
        } catch (RuntimeException ex) {
            metrics.auth("login", "failure");
            throw ex;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken, HttpServletRequest servletRequest) {
        String tokenPart = refreshToken == null || refreshToken.isBlank() ? "missing" : rateLimiter.digest(refreshToken);
        rateLimiter.enforce("refresh", clientIpResolver.resolve(servletRequest) + ":" + tokenPart);
        if (refreshToken == null || refreshToken.isBlank()) {
            metrics.auth("refresh", "failure");
            return invalidRefreshTokenResponse();
        }
        try {
            AuthService.LoginResult result = authService.refresh(refreshToken);
            metrics.auth("refresh", "success");
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie(result.rawRefreshToken()).toString())
                    .body(result.response());
        } catch (BadRequestException ex) {
            metrics.auth("refresh", "failure");
            return invalidRefreshTokenResponse();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        metrics.auth("logout", "success");
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal JwtPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authenticated principal is required");
        }
        return userRepository.findById(principal.userId())
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(user -> ResponseEntity.ok(new UserResponse(user.getId(), user.getEmail(), user.getRole())))
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private ResponseCookie refreshCookie(String rawToken) {
        return ResponseCookie.from(REFRESH_COOKIE, rawToken)
                .httpOnly(true)
                .secure(properties.isSecureCookies())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(properties.getRefreshTokenTtl())
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(properties.isSecureCookies())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseEntity<ProblemDetail> invalidRefreshTokenResponse() {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid refresh token");
        detail.setTitle("Invalid request");
        detail.setType(URI.create("https://example.com/problem/invalid-request"));
        return ResponseEntity.badRequest()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(detail);
    }

    private String normalizeForLimiter(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
