package com.evoq.ems.attendance.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AttendanceClockConfigTests {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(AttendanceClockConfig.class);

    @Test
    void defaultClockUsesColomboWithoutChangingJvmTimezone() {
        ZoneId original = ZoneId.systemDefault();
        context.run(application -> assertEquals(ZoneId.of("Asia/Colombo"),
                application.getBean(Clock.class).getZone()));
        assertEquals(original, ZoneId.systemDefault());
    }

    @Test
    void propertyOverridesBusinessTimezone() {
        context.withPropertyValues("ems.business-timezone=Pacific/Auckland")
                .run(application -> assertEquals(ZoneId.of("Pacific/Auckland"),
                        application.getBean(Clock.class).getZone()));
    }

    @Test
    void invalidTimezoneFailsRatherThanFallingBackToMachineZone() {
        assertThrows(java.time.DateTimeException.class,
                () -> new AttendanceClockConfig().attendanceClock("Invalid/Zone"));
    }
}
