package com.example.urlshortener.security;

import com.example.urlshortener.auth.config.AuthProperties;
import com.example.urlshortener.apikey.security.ApiKeyAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
                                            Rfc7807AuthenticationEntryPoint authenticationEntryPoint,
                                            Rfc7807AccessDeniedHandler accessDeniedHandler,
                                            CsrfCookieFilter csrfCookieFilter,
                                            ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                                            AuthProperties authProperties) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieName("XSRF-TOKEN");
        csrfTokenRepository.setHeaderName("X-XSRF-TOKEN");
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();

        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfRequestHandler)
                        .ignoringRequestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/urls/**",
                                "/api/v1/workspaces/**"
                        ))
                .addFilterAfter(csrfCookieFilter, CsrfFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, BearerTokenAuthenticationFilter.class)
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .requestMatcher(request -> authProperties.isSecureCookies())
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,
                                "/",
                                "/index.html",
                                "/app-config.json",
                                "/favicon.ico",
                                "/*.js",
                                "/*.css",
                                "/assets/**",
                                "/media/**",
                                "/login",
                                "/register",
                                "/app",
                                "/app/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/r/{shortCode}", "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness", "/actuator/info", "/v3/api-docs", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/metrics", "/actuator/metrics/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/analytics/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/audit/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/outbox/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/urls/{id}/block", "/api/v1/admin/urls/{id}/unblock").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/workspaces/{workspaceId}/campaigns/**", "/api/v1/workspaces/{workspaceId}/tags/**").hasAnyAuthority("ROLE_USER", "SCOPE_links:read", "SCOPE_links:write")
                        .requestMatchers(HttpMethod.POST, "/api/v1/workspaces/{workspaceId}/campaigns/**").hasRole("USER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/workspaces/{workspaceId}/campaigns/**").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/workspaces/{workspaceId}/campaigns/**").hasRole("USER")
                        .requestMatchers("/api/v1/auth/me", "/api/v1/workspaces/**").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/urls/{id}/analytics", "/api/v1/urls/{id}/analytics/daily").hasAnyAuthority("ROLE_USER", "SCOPE_analytics:read")
                        .requestMatchers(HttpMethod.GET, "/api/v1/urls/**").hasAnyAuthority("ROLE_USER", "SCOPE_links:read", "SCOPE_links:write")
                        .requestMatchers(HttpMethod.POST, "/api/v1/urls/**").hasAnyAuthority("ROLE_USER", "SCOPE_links:write")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/urls/**").hasAnyAuthority("ROLE_USER", "SCOPE_links:write")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/urls/**").hasAnyAuthority("ROLE_USER", "SCOPE_links:write")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/urls/**").hasAnyAuthority("ROLE_USER", "SCOPE_links:write")
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AuthProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "X-XSRF-TOKEN", "X-Correlation-ID", "X-Workspace-ID", "X-API-Key", "Idempotency-Key", HttpHeaders.IF_MATCH));
        configuration.setExposedHeaders(List.of("XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyAuthenticationFilterRegistration(
            ApiKeyAuthenticationFilter filter) {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
