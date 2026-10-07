package com.evoq.ems.common;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.evoq.ems.auth.DatabaseUserDetailsService;
import com.evoq.ems.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(SharedAccessDeniedApiTests.RestrictedController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, ApiExceptionHandler.class,
        SharedAccessDeniedApiTests.RestrictedController.class})
class SharedAccessDeniedApiTests {
    @Autowired MockMvc mvc;
    @MockitoBean DatabaseUserDetailsService users;

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void methodDenialOnReadReturnsShared403Contract() throws Exception {
        mvc.perform(get("/api/test/restricted"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/api/test/restricted"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void methodDenialOnWriteReturns403WithValidCsrf() throws Exception {
        mvc.perform(post("/api/test/restricted").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @WithMockUser(roles = "MANAGER_ADMIN")
    void allowedRoleStillSucceeds() throws Exception {
        mvc.perform(get("/api/test/restricted")).andExpect(status().isOk());
        mvc.perform(post("/api/test/restricted").with(csrf())).andExpect(status().isOk());
    }

    @Test
    void anonymousReadAndWriteStillRequireSignIn() throws Exception {
        mvc.perform(get("/api/test/restricted")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/test/restricted")).andExpect(status().isUnauthorized());
    }

    @RestController
    static class RestrictedController {
        @GetMapping("/api/test/restricted")
        @PreAuthorize("hasRole('MANAGER_ADMIN')")
        public String read() { return "allowed"; }

        @PostMapping("/api/test/restricted")
        @PreAuthorize("hasRole('MANAGER_ADMIN')")
        public String write() { return "allowed"; }
    }
}
