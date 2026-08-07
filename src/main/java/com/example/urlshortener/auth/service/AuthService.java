package com.example.urlshortener.auth.service;

import com.example.urlshortener.auth.dto.AuthDtos.AuthResponse;
import com.example.urlshortener.auth.dto.AuthDtos.LoginRequest;
import com.example.urlshortener.auth.dto.AuthDtos.RegisterRequest;
import com.example.urlshortener.auth.dto.AuthDtos.UserResponse;
import com.example.urlshortener.common.exception.BadRequestException;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.entity.UserRole;
import com.example.urlshortener.user.entity.UserStatus;
import com.example.urlshortener.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        validatePassword(request.password());
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered");
        }
        UserEntity user = new UserEntity(UUID.randomUUID(), email, passwordEncoder.encode(request.password()), UserRole.USER, UserStatus.ACTIVE);
        UserEntity saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmail(email)
                .filter(found -> found.getStatus() == UserStatus.ACTIVE)
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return issueLoginResult(user);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public LoginResult refresh(String rawRefreshToken) {
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.rotate(rawRefreshToken);
        return issueLoginResult(refreshToken.entity().getUser(), refreshToken.rawToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeIfPresent(rawRefreshToken);
    }

    public AuthResponse me(UserEntity user) {
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issue(user);
        return new AuthResponse(accessToken.value(), "Bearer", accessToken.expiresAt(), toUserResponse(user));
    }

    private LoginResult issueLoginResult(UserEntity user) {
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);
        return issueLoginResult(user, refreshToken.rawToken());
    }

    private LoginResult issueLoginResult(UserEntity user, String rawRefreshToken) {
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issue(user);
        AuthResponse response = new AuthResponse(accessToken.value(), "Bearer", accessToken.expiresAt(), toUserResponse(user));
        return new LoginResult(response, rawRefreshToken);
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BadRequestException("Email must be valid");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password is required");
        }
        if (!password.equals(password.strip())) {
            throw new BadRequestException("Password must not start or end with whitespace");
        }
        if (password.length() < 12 || password.length() > 128) {
            throw new BadRequestException("Password length must be between 12 and 128 characters");
        }
    }

    private UserResponse toUserResponse(UserEntity user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }

    public record LoginResult(AuthResponse response, String rawRefreshToken) {
    }
}
