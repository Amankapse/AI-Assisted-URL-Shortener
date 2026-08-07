package com.example.urlshortener.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.UUID;

@Component
public class Rfc7807AuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public Rfc7807AuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws java.io.IOException {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        detail.setTitle("Unauthorized");
        detail.setType(URI.create("https://example.com/problem/unauthorized"));
        detail.setProperty("correlationId", correlationId(request));
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), detail);
    }

    private String correlationId(HttpServletRequest request) {
        String header = request.getHeader("X-Correlation-ID");
        return header == null || header.isBlank() ? UUID.randomUUID().toString() : header;
    }
}
