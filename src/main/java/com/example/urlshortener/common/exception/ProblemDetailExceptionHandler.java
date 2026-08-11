package com.example.urlshortener.common.exception;

import com.example.urlshortener.common.correlation.CorrelationIdFilter;
import com.example.urlshortener.common.ratelimit.RateLimitExceededException;
import com.example.urlshortener.idempotency.service.IdempotencyConflictException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.stream.Collectors;

@ControllerAdvice
public class ProblemDetailExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Resource not found");
        detail.setType(URI.create("https://example.com/problem/resource-not-found"));
        detail.setProperty("correlationId", correlationId(request));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(detail);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(BadRequestException ex, WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("Invalid request");
        detail.setType(URI.create("https://example.com/problem/invalid-request"));
        detail.setProperty("correlationId", correlationId(request));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, violations);
        detail.setTitle("Validation failed");
        detail.setType(URI.create("https://example.com/problem/validation-failed"));
        detail.setProperty("correlationId", correlationId(request));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(detail);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> handleIdempotencyConflict(IdempotencyConflictException ex, WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setTitle("Conflict");
        detail.setType(URI.create("https://example.com/problem/idempotency-conflict"));
        detail.setProperty("errorCode", "idempotency_conflict");
        detail.setProperty("correlationId", correlationId(request));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
    }

    @ExceptionHandler(PreconditionRequiredException.class)
    public ResponseEntity<ProblemDetail> handlePreconditionRequired(PreconditionRequiredException ex, WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_REQUIRED, ex.getMessage());
        detail.setTitle("Precondition Required");
        detail.setType(URI.create("https://example.com/problem/precondition-required"));
        detail.setProperty("errorCode", "precondition_required");
        detail.setProperty("correlationId", correlationId(request));
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(detail);
    }

    @ExceptionHandler(PreconditionFailedException.class)
    public ResponseEntity<ProblemDetail> handlePreconditionFailed(PreconditionFailedException ex, WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_FAILED, ex.getMessage());
        detail.setTitle("Precondition Failed");
        detail.setType(URI.create("https://example.com/problem/precondition-failed"));
        detail.setProperty("errorCode", "precondition_failed");
        detail.setProperty("correlationId", correlationId(request));
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        String violations = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, violations);
        detail.setTitle("Validation failed");
        detail.setType(URI.create("https://example.com/problem/validation-failed"));
        detail.setProperty("correlationId", correlationId(request));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedRequest(HttpMessageNotReadableException ex, WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body is malformed or unreadable.");
        detail.setTitle("Malformed request");
        detail.setType(URI.create("https://example.com/problem/malformed-request"));
        detail.setProperty("correlationId", correlationId(request));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(detail);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(RateLimitExceededException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many requests.");
        detail.setTitle("Too Many Requests");
        detail.setType(URI.create("https://example.com/problem/rate-limit-exceeded"));
        detail.setProperty("errorCode", "rate_limit_exceeded");
        detail.setProperty("correlationId", CorrelationIdFilter.current(request));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()))
                .body(detail);
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ProblemDetail> handleQuotaExceeded(QuotaExceededException ex, WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        detail.setTitle("Quota exceeded");
        detail.setType(URI.create("https://example.com/problem/quota-exceeded"));
        detail.setProperty("errorCode", ex.code());
        detail.setProperty("correlationId", correlationId(request));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(detail);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(ForbiddenException ex, WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        detail.setTitle("Forbidden");
        detail.setType(URI.create("https://example.com/problem/forbidden"));
        detail.setProperty("errorCode", "forbidden");
        detail.setProperty("correlationId", correlationId(request));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(detail);
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ProblemDetail> handleAuthentication(Exception ex, WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        detail.setTitle("Unauthorized");
        detail.setType(URI.create("https://example.com/problem/unauthorized"));
        detail.setProperty("correlationId", correlationId(request));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(detail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        detail.setTitle("Internal server error");
        detail.setType(URI.create("https://example.com/problem/internal-server-error"));
        detail.setProperty("correlationId", correlationId(request));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(detail);
    }

    private String correlationId(WebRequest request) {
        String header = request.getHeader("X-Correlation-ID");
        if (request instanceof org.springframework.web.context.request.ServletWebRequest servletWebRequest) {
            return CorrelationIdFilter.current(servletWebRequest.getRequest());
        }
        if (header != null && !header.isBlank()) {
            return header;
        }
        return "unavailable";
    }
}
