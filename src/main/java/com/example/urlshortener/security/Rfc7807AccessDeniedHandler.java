package com.example.urlshortener.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.UUID;

@Component
public class Rfc7807AccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public Rfc7807AccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws java.io.IOException {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access is denied.");
        detail.setTitle("Forbidden");
        detail.setType(URI.create("https://example.com/problem/forbidden"));
        detail.setProperty("correlationId", correlationId(request));
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), detail);
    }

    private String correlationId(HttpServletRequest request) {
        String header = request.getHeader("X-Correlation-ID");
        return header == null || header.isBlank() ? UUID.randomUUID().toString() : header;
    }
}
