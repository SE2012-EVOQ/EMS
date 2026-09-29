package com.evoq.ems.attendance.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AttendanceClockConfig {
    @Bean
    Clock attendanceClock() { return Clock.systemDefaultZone(); }
}
