package com.evoq.ems.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Validate migration and close setup after any development/operator account provisioning. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class FirstRunSetupStartup implements ApplicationRunner {
    private final FirstRunSetupService setup;
    public FirstRunSetupStartup(FirstRunSetupService setup) { this.setup = setup; }
    @Override public void run(ApplicationArguments arguments) { setup.sealExistingInstallation(); }
}
