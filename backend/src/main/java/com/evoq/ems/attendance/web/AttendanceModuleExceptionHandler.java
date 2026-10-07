package com.evoq.ems.attendance.web;

import com.evoq.ems.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {AttendanceController.class, ScheduleController.class, com.evoq.ems.attendance.report.AttendanceReportController.class})
public class AttendanceModuleExceptionHandler {

    @ExceptionHandler(AttendanceModuleException.class)
    public ResponseEntity<ApiError> handle(AttendanceModuleException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status()).body(
                ApiError.of(exception.status(), exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> invalidQuery(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST, "Attendance query parameters are invalid", request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(AccessDeniedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiError.of(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI()));
    }
}
