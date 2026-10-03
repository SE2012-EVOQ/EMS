package com.evoq.ems.common;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError field : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(field.getField(), field.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ApiError.withFields(
                HttpStatus.BAD_REQUEST, "Please correct the highlighted fields", request.getRequestURI(), fields));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> badRequest(Exception exception, HttpServletRequest request) {
        String message = exception instanceof IllegalArgumentException
                ? exception.getMessage() : "Request body is invalid";
        return ResponseEntity.badRequest().body(ApiError.of(HttpStatus.BAD_REQUEST, message, request.getRequestURI()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> responseStatus(ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return ResponseEntity.status(status).body(ApiError.of(status,
                exception.getReason() == null ? status.getReasonPhrase() : exception.getReason(),
                request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled API error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI()));
    }
}
