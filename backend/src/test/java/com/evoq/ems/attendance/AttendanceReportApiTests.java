package com.evoq.ems.attendance;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import com.evoq.ems.attendance.report.*;
import com.evoq.ems.attendance.report.AttendanceReportDtos.Scope;
import com.evoq.ems.attendance.web.*;
import com.evoq.ems.auth.DatabaseUserDetailsService;
import com.evoq.ems.common.*;
import com.evoq.ems.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AttendanceReportController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, ApiExceptionHandler.class, AttendanceModuleExceptionHandler.class})
class AttendanceReportApiTests {
    @Autowired MockMvc mvc;
    @MockitoBean AttendanceReportService reports;
    @MockitoBean DatabaseUserDetailsService users;
    @Test void anonymousReportsCsvOptionsDashboardRequireAuthentication() throws Exception {
        for (String path : new String[]{"?from=2026-10-01&to=2026-10-07", "/csv?from=2026-10-01&to=2026-10-07", "/options", "/dashboard"})
            mvc.perform(get("/api/attendance-reports"+path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(reports);
    }
    @Test @WithMockUser(roles="EMPLOYEE") void csvUsesSameAuthorizedReportAndAttachment() throws Exception {
        var from=LocalDate.of(2026,10,1); var to=LocalDate.of(2026,10,7);
        when(reports.csv(null)).thenReturn("Employee ID,Employee\r\n");
        mvc.perform(get("/api/attendance-reports/csv?from=2026-10-01&to=2026-10-07"))
            .andExpect(status().isOk()).andExpect(content().contentType("text/csv;charset=UTF-8"))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=attendance-2026-10-01-2026-10-07.csv"));
        verify(reports).report(null, Scope.MINE, null, null, from, to);
    }
    @Test @WithMockUser(roles="EMPLOYEE") void serviceScopeRejectionAppliesToJsonAndCsv() throws Exception {
        when(reports.report(any(), eq(Scope.ORGANIZATION), any(), any(), any(), any()))
            .thenThrow(new AttendanceModuleException(HttpStatus.FORBIDDEN,"Access denied"));
        for (String path : new String[]{"", "/csv"}) mvc.perform(get("/api/attendance-reports"+path+"?scope=ORGANIZATION&from=2026-10-01&to=2026-10-07"))
            .andExpect(status().isForbidden());
    }
    @Test @WithMockUser void invalidFilterValuesAreBadRequest() throws Exception {
        mvc.perform(get("/api/attendance-reports?scope=INVALID&from=2026-10-01&to=2026-10-07")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/attendance-reports?from=bad&to=2026-10-07")).andExpect(status().isBadRequest());
    }
}
