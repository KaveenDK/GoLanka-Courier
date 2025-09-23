package lk.ijse.edu.golankacourier.exception;

/**
 * --------------------------------------------
 * @Author Dimantha Kaveen
 * @GitHub: https://github.com/KaveenDK
 * --------------------------------------------
 * @Created 8/30/2025
 * @Project GoLankaCourier
 * --------------------------------------------
 **/

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Global exception handler that returns consistent JSON error responses.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
        log.warn("API error: {} - {}", ex.getStatus(), ex.getMessage());
        ApiError err = new ApiError(ex.getStatus().value(), ex.getMessage(), ex.getErrorCode(), Instant.now(), request.getDescription(false));
        return new ResponseEntity<>(err, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        List<ApiValidationError> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError)
                .collect(Collectors.toList());

        String message = "Validation failed for " + validationErrors.size() + " field(s)";
        ApiError err = new ApiError(HttpStatus.BAD_REQUEST.value(), message, "VALIDATION_ERROR", Instant.now(), request.getDescription(false));
        err.setValidationErrors(validationErrors);
        return new ResponseEntity<>(err, HttpStatus.BAD_REQUEST);
    }

    private ApiValidationError toValidationError(FieldError fe) {
        return new ApiValidationError(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue() == null ? null : fe.getRejectedValue().toString());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String msg = String.format("Parameter '%s' expected type %s but received value '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                ex.getValue());
        ApiError err = new ApiError(HttpStatus.BAD_REQUEST.value(), msg, "TYPE_MISMATCH", Instant.now(), request.getDescription(false));
        return new ResponseEntity<>(err, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        ApiError err = new ApiError(HttpStatus.BAD_REQUEST.value(), "Malformed JSON request", "MALFORMED_JSON", Instant.now(), request.getDescription(false));
        return new ResponseEntity<>(err, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ApiError err = new ApiError(HttpStatus.UNAUTHORIZED.value(), "Authentication failed", "AUTH_FAILURE", Instant.now(), request.getDescription(false));
        return new ResponseEntity<>(err, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        ApiError err = new ApiError(HttpStatus.FORBIDDEN.value(), "Access denied", "ACCESS_DENIED", Instant.now(), request.getDescription(false));
        return new ResponseEntity<>(err, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleAllUncaught(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        ApiError err = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred", "INTERNAL_ERROR", Instant.now(), request.getDescription(false));
        return new ResponseEntity<>(err, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // --- helper DTOs for error responses ---

    public static class ApiError {
        private int status;
        private String message;
        private String code;
        private Instant timestamp;
        private String path;
        private List<ApiValidationError> validationErrors;

        public ApiError(int status, String message, String code, Instant timestamp, String path) {
            this.status = status;
            this.message = message;
            this.code = code;
            this.timestamp = timestamp;
            this.path = path;
        }

        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public String getCode() { return code; }
        public Instant getTimestamp() { return timestamp; }
        public String getPath() { return path; }
        public List<ApiValidationError> getValidationErrors() { return validationErrors; }
        public void setValidationErrors(List<ApiValidationError> validationErrors) { this.validationErrors = validationErrors; }
    }

    public static class ApiValidationError {
        private String field;
        private String message;
        private String rejectedValue;

        public ApiValidationError(String field, String message, String rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        public String getField() { return field; }
        public String getMessage() { return message; }
        public String getRejectedValue() { return rejectedValue; }
    }
}
