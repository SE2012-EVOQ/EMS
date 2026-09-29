package com.evoq.ems.employee.web;

import org.springframework.http.HttpStatus;

public class EmployeeModuleException extends RuntimeException {

    private final HttpStatus status;

    public EmployeeModuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static EmployeeModuleException notFound(String message) {
        return new EmployeeModuleException(HttpStatus.NOT_FOUND, message);
    }

    public static EmployeeModuleException badRequest(String message) {
        return new EmployeeModuleException(HttpStatus.BAD_REQUEST, message);
    }

    public static EmployeeModuleException conflict(String message) {
        return new EmployeeModuleException(HttpStatus.CONFLICT, message);
    }

    public static EmployeeModuleException forbidden(String message) {
        return new EmployeeModuleException(HttpStatus.FORBIDDEN, message);
    }
}
