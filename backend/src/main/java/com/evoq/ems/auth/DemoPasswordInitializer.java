package com.evoq.ems.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
@Profile("dev")
public class DemoPasswordInitializer {

    private static final Logger log = LoggerFactory.getLogger(DemoPasswordInitializer.class);
    private static final String PLACEHOLDER = "DEMO_HASH_REPLACE_DURING_SETUP";

    @Bean
    ApplicationRunner initializeDemoPasswords(UserAccountRepository accounts, PasswordEncoder encoder,
            @Value("${DEMO_PASSWORD:}") String demoPassword) {
        return arguments -> {
            if (!StringUtils.hasText(demoPassword)) {
                log.warn("DEMO_PASSWORD is unset; placeholder demo accounts cannot sign in");
                return;
            }
            int initialized = accounts.replacePlaceholderHashes(PLACEHOLDER, encoder.encode(demoPassword));
            log.info("Initialized {} placeholder demo account passwords", initialized);
        };
    }
}
