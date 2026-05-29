package com.cognizant.TransactionService.exception;

import com.cognizant.TransactionService.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ----404----
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TransactionNotFoundException ex, HttpServletRequest req) {
        log.warn("Not found {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "TXN_NOT_FOUND", ex.getMessage(), req, null);
    }

    // ----409 - duplicate idempotency key ----
    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateTransactionException ex, HttpServletRequest req) {
        log.warn("Duplicate: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "DUPLICATE_IDEMPOTENCY_KEY", ex.getMessage(), req, null);
    }

    // ----400 - business rule violations ----
    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(InvalidTransactionException ex, HttpServletRequest req) {
        log.warn("Invalid Transaction: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION", ex.getMessage(), req, null);
    }

    // ----422 - bean validation ----
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        log.warn("Validation failed: {}", fieldErrors);
        return build(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED", ex.getMessage(), req, fieldErrors);
    }

    // ----400 - missing required request parameters ----
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest req) {
        String message = "Required parameter '" + ex.getParameterName() + "' is missing";
        log.warn("Missing parameter: {}", message);
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", message, req, null);
    }

    @ExceptionHandler(DeniedAccessException.class)
    public ResponseEntity<ErrorResponse> handleDeniedAccess(
            DeniedAccessException ex, HttpServletRequest req) {

        log.warn("FORBIDDEN: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "DENIED_ACCESS", ex.getMessage(), req, null);
    }

    // ----500 - catch all ----
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled Exception on {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req, null);

    }

    public ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, HttpServletRequest req, Map<String, String> fieldErrors) {
        ErrorResponse body = new ErrorResponse()
                .builder()
                .timeStamp(Instant.now())
                .status(status.value())
                .errorCode(code)
                .message(message)
                .path(req.getRequestURI())
                .errors(fieldErrors)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}
