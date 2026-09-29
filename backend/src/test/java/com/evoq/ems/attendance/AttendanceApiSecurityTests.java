package com.evoq.ems.attendance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.evoq.ems.attendance.domain.AttendanceRecord;
import com.evoq.ems.attendance.service.AttendanceService;
import com.evoq.ems.attendance.web.AttendanceController;
import com.evoq.ems.attendance.web.AttendanceDtos.RecordResponse;
import com.evoq.ems.attendance.web.AttendanceModuleExceptionHandler;
import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.auth.DatabaseUserDetailsService;
import com.evoq.ems.common.ApiErrorWriter;
import com.evoq.ems.common.ApiExceptionHandler;
import com.evoq.ems.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AttendanceController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, ApiExceptionHandler.class,
        AttendanceModuleExceptionHandler.class})
class AttendanceApiSecurityTests {

    @Autowired MockMvc mvc;
    @MockitoBean AttendanceService attendance;
    @MockitoBean DatabaseUserDetailsService users;

    @Test
    void anonymousAccessIsRejected() throws Exception {
        mvc.perform(get("/api/attendance-records/me?from=2026-09-01&to=2026-09-30"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotReadAllRecordsOrWrite() throws Exception {
        mvc.perform(get("/api/attendance-records?from=2026-09-01&to=2026-09-30"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/attendance-records/exceptions").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":3,\"date\":\"2026-09-29\",\"status\":\"ABSENT\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void selfCheckInCommandIgnoresClientSuppliedAttendanceFields() throws Exception {
        when(attendance.checkIn(null)).thenReturn(new RecordResponse(9L, 3L, "Demo Employee",
                LocalDate.of(2026, 9, 29), AttendanceRecord.Status.PRESENT, java.time.LocalTime.of(9, 0),
                null, new BigDecimal("0.00"), null));
        mvc.perform(post("/api/attendance-records/check-in").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":99,\"date\":\"2000-01-01\",\"checkIn\":\"00:00:00\",\"hours\":999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value(3));
        verify(attendance).checkIn(null);
    }

    @Test
    @WithMockUser(roles = "MANAGER_ADMIN")
    void managerCanCreateExceptionalRecordWithCsrf() throws Exception {
        when(attendance.createException(any(), any())).thenReturn(
                new RecordResponse(9L, 3L, "Demo Employee", LocalDate.of(2026, 9, 29),
                        AttendanceRecord.Status.ABSENT, null, null, new BigDecimal("0.00"), null));
        mvc.perform(post("/api/attendance-records/exceptions").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":3,\"date\":\"2026-09-29\",\"status\":\"ABSENT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(3))
                .andExpect(jsonPath("$.hours").value(0.0));
    }

    @Test
    @WithMockUser(roles = "MANAGER_ADMIN")
    void invalidDateQueryIsBadRequest() throws Exception {
        when(attendance.all(any(), any(), any(), any())).thenReturn(List.of());
        mvc.perform(get("/api/attendance-records?from=bad&to=2026-09-30"))
                .andExpect(status().isBadRequest());
    }
}
