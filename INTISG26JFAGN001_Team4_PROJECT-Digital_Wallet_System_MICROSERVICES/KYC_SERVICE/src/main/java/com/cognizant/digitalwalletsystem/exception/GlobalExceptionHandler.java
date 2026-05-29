package com.cognizant.digitalwalletsystem.exception;

import jakarta.servlet.http.HttpServletRequest;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------ //
    //  KYC Domain Exceptions
    // ------------------------------------------------------------------ //

    @ExceptionHandler(KycNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleKycNotFound(
            KycNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(KycAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleKycAlreadyExists(
            KycAlreadyExistsException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UserNotRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleUserNotRegistered(
            UserNotRegisteredException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidDocumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDocument(
            InvalidDocumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(KycStatusException.class)
    public ResponseEntity<ErrorResponse> handleKycStatus(
            KycStatusException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // ------------------------------------------------------------------ //
    //  Spring MVC / HTTP Exceptions
    // ------------------------------------------------------------------ //

    /**
     * Wrong / non-existent URL  →  404
     * e.g. /api/kyc/swagger-ui.html instead of /swagger-ui.html
     *
     * NOTE: NoResourceFoundException was introduced in Spring Framework 6.1
     * (Spring Boot 3.2+). It replaces the old NoHandlerFoundException for
     * static-resource-not-found scenarios.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "The requested URL does not exist: " + request.getRequestURI()
                + ". Please check the correct API path.",
                request.getRequestURI());
    }


    @ExceptionHandler(DeniedAccessException.class)
    public ResponseEntity<ErrorResponse> handleDeniedAccess(
            DeniedAccessException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.FORBIDDEN,ex.getMessage(),request.getRequestURI());
    }

    /*
     * NOTE — HTTP 405 (Method Not Allowed) is intentionally NOT handled here.
     *
     * In Spring Framework 6.2 (Spring Boot 3.4.x), HttpRequestMethodNotAllowedException
     * was removed from org.springframework.web.  Spring 6.2 now returns the 405
     * response directly inside DefaultHandlerExceptionResolver without throwing
     * an exception visible to @RestControllerAdvice.
     *
     * Result: calling GET on /api/kyc/submit (POST-only) will still correctly
     * return HTTP 405 with the "Allow" header — handled natively by Spring MVC.
     */

    /**
     * Malformed / unreadable JSON body  →  400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON in request body. Please verify the request format.",
                request.getRequestURI());
    }

    /**
     * Wrong type for @PathVariable or @RequestParam  →  400
     * e.g. GET /api/kyc/admin/abc  (abc is not a valid Long)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName()
                + "'. Expected type: "
                + (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"),
                request.getRequestURI());
    }

    /**
     * Missing required @RequestParam  →  400
     * e.g. PUT /api/kyc/update/{userId}  without ?role=
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Required request parameter '" + ex.getParameterName() + "' is missing.",
                request.getRequestURI());
    }

    // ------------------------------------------------------------------ //
    //  Bean Validation
    // ------------------------------------------------------------------ //

    /**
     * @Valid on @RequestBody fails  →  400 with per-field error map
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more fields are invalid. See 'fieldErrors' for details.")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ------------------------------------------------------------------ //
    //  Generic Fallback
    // ------------------------------------------------------------------ //

    /**
     * Feign call to User Service failed (connection refused, timeout, etc.)  →  503
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(
            FeignException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "User Service is currently unavailable. Please try again later. Cause: "
                + ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + ex.getMessage(),
                request.getRequestURI());
    }

    // ------------------------------------------------------------------ //
    //  Helper
    // ------------------------------------------------------------------ //

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();
        return ResponseEntity.status(status).body(body);
    }


}
