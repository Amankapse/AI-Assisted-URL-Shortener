package com.example.urlshortener.apikey.security;

import com.example.urlshortener.apikey.entity.ApiKeyScope;
import com.example.urlshortener.apikey.service.ApiKeyService;
import com.example.urlshortener.common.correlation.CorrelationIdFilter;
import com.example.urlshortener.common.ratelimit.RateLimitExceededException;
import com.example.urlshortener.security.Rfc7807AuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-API-Key";

    private final ApiKeyService apiKeyService;
    private final Rfc7807AuthenticationEntryPoint authenticationEntryPoint;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService,
                                      Rfc7807AuthenticationEntryPoint authenticationEntryPoint,
                                      ObjectMapper objectMapper) {
        this.apiKeyService = apiKeyService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/r/")
                || path.startsWith("/actuator/health")
                || path.equals("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String rawKey = request.getHeader(HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            authenticationEntryPoint.commence(request, response, new BadCredentialsException("Invalid credentials"));
            return;
        }
        try {
            ApiKeyPrincipal principal = apiKeyService.authenticate(rawKey);
            ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                    principal,
                    principal.scopes().stream()
                            .map(ApiKeyScope::value)
                            .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                            .toList()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (BadCredentialsException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, ex);
        } catch (RateLimitExceededException ex) {
            SecurityContextHolder.clearContext();
            writeRateLimitResponse(request, response, ex);
        } finally {
            rawKey = null;
        }
    }

    private void writeRateLimitResponse(HttpServletRequest request, HttpServletResponse response,
                                        RateLimitExceededException ex) throws IOException {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many requests.");
        detail.setTitle("Too Many Requests");
        detail.setType(URI.create("https://example.com/problem/rate-limit-exceeded"));
        detail.setProperty("errorCode", "rate_limit_exceeded");
        detail.setProperty("correlationId", CorrelationIdFilter.current(request));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), detail);
    }
}
