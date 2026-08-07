package com.example.urlshortener.auth.controller;

import com.example.urlshortener.auth.config.AuthProperties;
import com.example.urlshortener.auth.dto.AuthDtos.AuthResponse;
import com.example.urlshortener.auth.dto.AuthDtos.LoginRequest;
import com.example.urlshortener.auth.dto.AuthDtos.RegisterRequest;
import com.example.urlshortener.auth.dto.AuthDtos.UserResponse;
import com.example.urlshortener.auth.service.AuthService;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.common.exception.ResourceNotFoundException;
import com.example.urlshortener.security.JwtPrincipal;
import com.example.urlshortener.user.entity.UserStatus;
import com.example.urlshortener.user.repository.UserRepository;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuthProperties properties;

    public AuthController(AuthService authService, UserRepository userRepository, AuthProperties properties) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result.rawRefreshToken()).toString())
                .body(result.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return invalidRefreshTokenResponse();
        }
        try {
            AuthService.LoginResult result = authService.refresh(refreshToken);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie(result.rawRefreshToken()).toString())
                    .body(result.response());
        } catch (BadRequestException ex) {
            return invalidRefreshTokenResponse();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
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
}
