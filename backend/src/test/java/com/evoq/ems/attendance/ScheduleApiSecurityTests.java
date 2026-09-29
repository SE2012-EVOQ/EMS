package com.evoq.ems.attendance;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.evoq.ems.attendance.service.ScheduleService;
import com.evoq.ems.attendance.web.AttendanceModuleExceptionHandler;
import com.evoq.ems.attendance.web.ScheduleController;
import com.evoq.ems.attendance.web.ScheduleDtos.TeamResponse;
import com.evoq.ems.auth.DatabaseUserDetailsService;
import com.evoq.ems.common.ApiErrorWriter;
import com.evoq.ems.common.ApiExceptionHandler;
import com.evoq.ems.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ScheduleController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, ApiExceptionHandler.class, AttendanceModuleExceptionHandler.class})
class ScheduleApiSecurityTests {
    @Autowired MockMvc mvc;
    @MockitoBean ScheduleService schedules;
    @MockitoBean DatabaseUserDetailsService users;

    @Test
    void anonymousScheduleReadRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/schedules/me?from=2026-09-01&to=2026-09-30")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotCreateOrListSupervisorTeamSchedules() throws Exception {
        mvc.perform(get("/api/schedules/teams")).andExpect(status().isForbidden());
        mvc.perform(post("/api/schedules").with(csrf()).contentType("application/json")
                .content("{\"teamId\":7,\"periodStart\":\"2026-09-01\",\"periodEnd\":\"2026-09-30\",\"entries\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void supervisorCanReadManagedTeams() throws Exception {
        when(schedules.managedTeams(null)).thenReturn(List.of(new TeamResponse(7L, "Development Scheduling Team")));
        mvc.perform(get("/api/schedules/teams")).andExpect(status().isOk());
    }
}
