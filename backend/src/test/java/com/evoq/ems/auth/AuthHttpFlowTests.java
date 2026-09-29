package com.evoq.ems.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.evoq.ems.common.ApiErrorWriter;
import com.evoq.ems.common.ApiExceptionHandler;
import com.evoq.ems.config.SecurityConfig;
import com.evoq.ems.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = {AuthController.class, HealthController.class})
@Import({SecurityConfig.class, ApiErrorWriter.class, ApiExceptionHandler.class})
class AuthHttpFlowTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PasswordEncoder encoder;
    @MockitoBean DatabaseUserDetailsService users;
    @MockitoBean PasswordService passwords;

    @Test
    void sessionLoginCurrentUserAndLogout() throws Exception {
        AccountPrincipal active = principal(true, "maya.fernando");
        when(users.loadUserByUsername("maya.fernando")).thenReturn(active);
        MvcResult csrf = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = mapper.readTree(csrf.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) csrf.getRequest().getSession(false);

        MvcResult login = mvc.perform(withCsrf(post("/api/auth/login")
                .session(session).param("username", "maya.fernando").param("password", "correct-password"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.employeeId").value(1))
                .andExpect(jsonPath("$.role").value("MANAGER_ADMIN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        session = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("maya.fernando"));

        MvcResult freshCsrf = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode logoutToken = mapper.readTree(freshCsrf.getResponse().getContentAsString());
        mvc.perform(withCsrf(post("/api/auth/change-password").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"correct-password\",\"newPassword\":\"new-password\"}"), logoutToken))
                .andExpect(status().isNoContent());
        verify(passwords).changePassword(eq("maya.fernando"), argThat(request ->
                request.currentPassword().equals("correct-password")
                        && request.newPassword().equals("new-password")));

        mvc.perform(withCsrf(post("/api/auth/logout").session(session), logoutToken))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicHealthProtectedUserAndCsrf() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login")).andExpect(status().isForbidden());
        mvc.perform(options("/api/health")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void badUnknownAndInactiveAccountsCannotLogin() throws Exception {
        AccountPrincipal active = principal(true, "maya.fernando");
        AccountPrincipal inactive = principal(false, "inactive");
        when(users.loadUserByUsername("maya.fernando")).thenReturn(active);
        when(users.loadUserByUsername("unknown")).thenThrow(new UsernameNotFoundException("unknown"));
        when(users.loadUserByUsername("inactive")).thenReturn(inactive);

        failedLogin("maya.fernando", "wrong-password");
        failedLogin("unknown", "correct-password");
        failedLogin("inactive", "correct-password");
    }

    private void failedLogin(String username, String password) throws Exception {
        MvcResult csrf = mvc.perform(get("/api/auth/csrf")).andReturn();
        JsonNode token = mapper.readTree(csrf.getResponse().getContentAsString());
        mvc.perform(withCsrf(post("/api/auth/login")
                .session((MockHttpSession) csrf.getRequest().getSession(false))
                .param("username", username).param("password", password), token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request, JsonNode token) {
        return request.header(token.get("headerName").asText(), token.get("token").asText());
    }

    private AccountPrincipal principal(boolean active, String username) {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn("MANAGER_ADMIN");
        UserAccount account = mock(UserAccount.class);
        when(account.getId()).thenReturn(1L);
        when(account.getEmployeeId()).thenReturn(1L);
        when(account.getUsername()).thenReturn(username);
        when(account.getPasswordHash()).thenReturn(encoder.encode("correct-password"));
        when(account.getRole()).thenReturn(role);
        when(account.isActive()).thenReturn(active);
        return new AccountPrincipal(account);
    }
}
