package com.evoq.ems.attendance.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AttendanceClockConfig {
    @Bean
    Clock attendanceClock(@Value("${ems.business-timezone:Asia/Colombo}") String businessTimezone) {
        return Clock.system(ZoneId.of(businessTimezone));
    }
}
