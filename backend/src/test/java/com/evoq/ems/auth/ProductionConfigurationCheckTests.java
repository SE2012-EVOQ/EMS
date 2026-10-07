package com.evoq.ems.auth;
import static org.junit.jupiter.api.Assertions.*;
import com.evoq.ems.config.ProductionConfigurationCheck;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
class ProductionConfigurationCheckTests {
    @Test void explicitHttpsOriginSecureCookiesAndExternalPasswordPass() {
        new ProductionConfigurationCheck(new MockEnvironment(),"https://ems.example.org","external-test-only",true).run(new DefaultApplicationArguments());
    }
    @Test void unsafeOriginsCookiesAndMissingPasswordRefuse() {
        for(String origin:new String[]{"", "http://localhost:5173", "https://ems.example.org/path", "https://user@ems.example.org", "https://ems.example.org?x=1"})
            assertThrows(IllegalStateException.class,()->new ProductionConfigurationCheck(new MockEnvironment(),origin,"external-test-only",true).run(new DefaultApplicationArguments()));
        assertThrows(IllegalStateException.class,()->new ProductionConfigurationCheck(new MockEnvironment(),"https://ems.example.org","",true).run(new DefaultApplicationArguments()));
        assertThrows(IllegalStateException.class,()->new ProductionConfigurationCheck(new MockEnvironment(),"https://ems.example.org","external-test-only",false).run(new DefaultApplicationArguments()));
    }
    @Test void devAndProdCannotRunTogether() {
        var environment=new MockEnvironment(); environment.setActiveProfiles("prod","dev");
        assertThrows(IllegalStateException.class,()->new ProductionConfigurationCheck(environment,"https://ems.example.org","external-test-only",true).run(new DefaultApplicationArguments()));
    }
}
