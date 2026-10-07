package com.evoq.ems.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class ProductionConfigurationCheck implements ApplicationRunner {
    private final Environment environment;
    private final String origin, password;
    private final boolean secure;
    public ProductionConfigurationCheck(Environment environment,
            @Value("${FRONTEND_ORIGIN:}") String origin,
            @Value("${spring.datasource.password:}") String password,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        this.environment = environment; this.origin = origin; this.password = password; this.secure = secure;
    }
    @Override
    public void run(ApplicationArguments arguments) {
        if (environment.acceptsProfiles(Profiles.of("dev"))) throw new IllegalStateException("Never combine prod and dev profiles");
        java.net.URI uri;
        try { uri = java.net.URI.create(origin); } catch (IllegalArgumentException ex) { throw new IllegalStateException("Production requires an explicit HTTPS frontend origin"); }
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getRawQuery() != null
                || uri.getFragment() != null || uri.getUserInfo() != null || (uri.getPath() != null && !uri.getPath().isEmpty()))
            throw new IllegalStateException("Production requires an explicit HTTPS frontend origin without a path");
        if (!secure || password.isBlank()) throw new IllegalStateException("Production requires secure session cookies and an external database password");
    }
}
