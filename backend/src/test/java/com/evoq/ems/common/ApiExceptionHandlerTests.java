package com.evoq.ems.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ApiExceptionHandlerTests {
    @Test
    void leaveConflictKeepsItsHttpStatusAndMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/leave/requests/7/approve");

        var response = new ApiExceptionHandler().responseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT, "Attendance already exists"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Attendance already exists", response.getBody().message());
    }
}
