package com.example.urlshortener.security;

import com.example.urlshortener.auth.config.AuthProperties;
import com.example.urlshortener.user.entity.UserRole;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;

@Configuration
@EnableMethodSecurity
public class JwtConfig {
    @Bean
    KeyPair jwtKeyPair(AuthProperties properties, Environment environment) {
        if (hasText(properties.getPrivateKeyPem()) && hasText(properties.getPublicKeyPem())) {
            return new KeyPair(parsePublicKey(properties.getPublicKeyPem()), parsePrivateKey(properties.getPrivateKeyPem()));
        }
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            return generateTestKeyPair();
        }
        throw new IllegalStateException("JWT RSA keys must be provided through APP_AUTH_PRIVATE_KEY_PEM and APP_AUTH_PUBLIC_KEY_PEM");
    }

    @Bean
    JwtEncoder jwtEncoder(KeyPair jwtKeyPair, AuthProperties properties) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) jwtKeyPair.getPublic())
                .privateKey((RSAPrivateKey) jwtKeyPair.getPrivate())
                .keyID(properties.getKeyId())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
    }

    @Bean
    JwtDecoder jwtDecoder(KeyPair jwtKeyPair, AuthProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) jwtKeyPair.getPublic())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.getIssuer()),
                audienceValidator(properties.getAudience()),
                requiredClaimsValidator()
        );
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            UserRole role = UserRole.valueOf(jwt.getClaimAsString("role"));
            JwtPrincipal principal = new JwtPrincipal(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("email"), role);
            return new JwtOwnerAuthenticationToken(
                    principal,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name())),
                    jwt
            );
        };
    }

    private OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
        return jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
    }

    private OAuth2TokenValidator<Jwt> requiredClaimsValidator() {
        return jwt -> {
            List<String> missing = new ArrayList<>();
            for (String claim : List.of("sub", "email", "role", "iss", "aud", "iat", "exp", "jti")) {
                if (!jwt.hasClaim(claim)) {
                    missing.add(claim);
                }
            }
            if (jwt.getExpiresAt() == null || jwt.getExpiresAt().isBefore(Instant.now())) {
                missing.add("valid exp");
            }
            return missing.isEmpty()
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing required claims: " + missing, null));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private RSAPrivateKey parsePrivateKey(String pem) {
        try {
            byte[] bytes = decodePem(pem, "PRIVATE KEY");
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid JWT private key", ex);
        }
    }

    private RSAPublicKey parsePublicKey(String pem) {
        try {
            byte[] bytes = decodePem(pem, "PUBLIC KEY");
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid JWT public key", ex);
        }
    }

    private byte[] decodePem(String pem, String type) {
        String normalized = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replace("\\n", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private KeyPair generateTestKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate test JWT key pair", ex);
        }
    }
}
