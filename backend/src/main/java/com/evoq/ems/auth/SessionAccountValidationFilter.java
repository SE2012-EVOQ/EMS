package com.evoq.ems.auth;

import java.io.IOException;
import java.util.Objects;
import com.evoq.ems.common.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revalidate cached session identity before CSRF and endpoint authorization. */
public class SessionAccountValidationFilter extends OncePerRequestFilter {
    private final DatabaseUserDetailsService users;
    private final ApiErrorWriter errors;
    public SessionAccountValidationFilter(DatabaseUserDetailsService users, ApiErrorWriter errors) {
        this.users = users; this.errors = errors;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal cached) {
            AccountPrincipal current = null;
            try { current = (AccountPrincipal) users.loadUserByUsername(cached.getUsername()); }
            catch (UsernameNotFoundException ignored) { }
            if (current == null || !current.isEnabled()
                    || !Objects.equals(cached.getUserId(), current.getUserId())
                    || !Objects.equals(cached.getEmployeeId(), current.getEmployeeId())
                    || !Objects.equals(cached.getRole(), current.getRole())
                    || !Objects.equals(cached.getPassword(), current.getPassword())) {
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
                errors.write(response, HttpStatus.UNAUTHORIZED, "Your account changed or your session expired. Sign in again.", request.getRequestURI());
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
