package com.evoq.ems.attendance.web;

import org.springframework.http.HttpStatus;

public class AttendanceModuleException extends RuntimeException {
    private final HttpStatus status;

    public AttendanceModuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() { return status; }
}
