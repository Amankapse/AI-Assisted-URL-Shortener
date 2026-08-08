package com.example.urlshortener.auth;

import com.example.urlshortener.analytics.repository.ClickEventRepository;
import com.example.urlshortener.auth.repository.RefreshTokenRepository;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.example.urlshortener.user.entity.UserEntity;
import com.example.urlshortener.user.entity.UserRole;
import com.example.urlshortener.user.entity.UserStatus;
import com.example.urlshortener.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ShortUrlRepository shortUrlRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired ClickEventRepository clickEventRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtEncoder jwtEncoder;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerShouldCreateUserSafelyAndRejectInvalidInputs() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "USER@Example.COM ", "password", "correct-horse-password"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        UserEntity user = userRepository.findByEmail("user@example.com").orElseThrow();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getPasswordHash()).doesNotContain("correct-horse-password");
        assertThat(passwordEncoder.matches("correct-horse-password", user.getPasswordHash())).isTrue();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "user@example.com", "password", "correct-horse-password"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "not-email", "password", "correct-horse-password"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "weak@example.com", "password", "short"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginShouldReturnAccessTokenAndSecureRefreshCookieWithGenericFailures() throws Exception {
        register("login@example.com", "correct-horse-password");

        MvcResult result = login("login@example.com", "correct-horse-password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.email").value("login@example.com"))
                .andExpect(content().string(not(containsString("password"))))
                .andReturn();
        assertThat(result.getResponse().getCookie("refresh_token")).isNotNull();
        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anyMatch(header -> header.contains("refresh_token=") && header.contains("HttpOnly") && header.contains("SameSite=Strict"));

        login("login@example.com", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));

        login("unknown@example.com", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));

        UserEntity user = userRepository.findByEmail("login@example.com").orElseThrow();
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(user);
        login("login@example.com", "correct-horse-password")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenShouldAuthenticateMeAndRejectTamperingAndWrongClaims() throws Exception {
        register("jwt@example.com", "correct-horse-password");
        String token = accessToken("jwt@example.com", "correct-horse-password");

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jwt@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + customToken("wrong-issuer", "url-shortener-api", userRepository.findByEmail("jwt@example.com").orElseThrow(), Instant.now().plusSeconds(300), UserRole.USER)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + customToken("url-shortener", "wrong-audience", userRepository.findByEmail("jwt@example.com").orElseThrow(), Instant.now().plusSeconds(300), UserRole.USER)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + customToken("url-shortener", "url-shortener-api", userRepository.findByEmail("jwt@example.com").orElseThrow(), Instant.now().minusSeconds(1), UserRole.USER)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutRole(userRepository.findByEmail("jwt@example.com").orElseThrow())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + hs256Token(userRepository.findByEmail("jwt@example.com").orElseThrow())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshAndLogoutShouldRequireCsrfAndRotateTokens() throws Exception {
        register("refresh@example.com", "correct-horse-password");
        MvcResult login = login("refresh@example.com", "correct-horse-password").andExpect(status().isOk()).andReturn();
        Cookie refreshCookie = login.getResponse().getCookie("refresh_token");
        Cookie csrfCookie = csrfCookie();

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isForbidden());

        MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        Cookie rotatedCookie = refresh.getResponse().getCookie("refresh_token");
        assertThat(rotatedCookie).isNotNull();
        assertThat(rotatedCookie.getValue()).isNotEqualTo(refreshCookie.getValue());
        assertThat(refreshTokenRepository.findAll()).anyMatch(token -> token.getRevokedAt() != null);
        UUID familyId = refreshTokenRepository.findAll().getFirst().getFamilyId();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        assertThat(refreshTokenRepository.countByFamilyIdAndRevokedAtIsNull(familyId)).isZero();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(rotatedCookie, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());
    }

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }

    private String accessToken(String email, String password) throws Exception {
        String body = login(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private Cookie csrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health")).andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private String customToken(String issuer, String audience, UserEntity user, Instant expiresAt, UserRole role) {
        Instant now = expiresAt.isBefore(Instant.now()) ? expiresAt.minusSeconds(60) : Instant.now();
        JwsHeader header = JwsHeader.with(() -> "RS256").type("JWT").keyId("local-dev").build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", role.name())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String tokenWithoutRole(UserEntity user) {
        JwsHeader header = JwsHeader.with(() -> "RS256").type("JWT").keyId("local-dev").build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("url-shortener")
                .audience(List.of("url-shortener-api"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String hs256Token(UserEntity user) throws Exception {
        com.nimbusds.jwt.JWTClaimsSet claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .issuer("url-shortener")
                .audience("url-shortener-api")
                .issueTime(java.util.Date.from(Instant.now()))
                .expirationTime(java.util.Date.from(Instant.now().plusSeconds(300)))
                .jwtID(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", UserRole.USER.name())
                .build();
        com.nimbusds.jwt.SignedJWT token = new com.nimbusds.jwt.SignedJWT(
                new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.HS256)
                        .type(com.nimbusds.jose.JOSEObjectType.JWT)
                        .build(),
                claims
        );
        token.sign(new com.nimbusds.jose.crypto.MACSigner("01234567890123456789012345678901"));
        return token.serialize();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
