package com.linkshort.exception;

import com.linkshort.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler using @RestControllerAdvice.
 *
 * Catches all custom exceptions and Spring validation errors,
 * returning consistent ErrorResponse JSON with appropriate HTTP status codes.
 *
 * WHY CENTRALIZED?
 * - Controllers stay clean (no try-catch blocks)
 * - Consistent error format across all endpoints
 * - Single place to add logging/monitoring
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUrlNotFound(UrlNotFoundException ex) {
        log.warn("URL not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<ErrorResponse> handleUrlExpired(UrlExpiredException ex) {
        log.warn("URL expired: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.GONE)  // 410 Gone — the resource existed but is no longer available
                .body(ErrorResponse.of(410, "Gone", ex.getMessage()));
    }

    @ExceptionHandler(CustomAliasExistsException.class)
    public ResponseEntity<ErrorResponse> handleCustomAliasExists(CustomAliasExistsException ex) {
        log.warn("Alias conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)  // 409 Conflict
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded");
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)  // 429
                .body(ErrorResponse.of(429, "Too Many Requests", ex.getMessage()));
    }

    /**
     * Handle @Valid annotation failures (e.g., @NotBlank, @Pattern on DTOs).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Validation Error", message));
    }

    /**
     * Handle constraint violations from path variables / request params.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Validation Error", ex.getMessage()));
    }

    /**
     * Handle business rule violations (e.g., URL validation, access control, ownership).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    /**
     * Catch-all for unexpected errors.
     * Logs the full stack trace but returns a generic message to the client
     * (never expose internal details in production).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error",
                        "An unexpected error occurred. Please try again later."));
    }
}
