package com.evoq.ems.attendance.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AttendanceClockConfig {
    @Bean
    Clock attendanceClock() { return Clock.systemDefaultZone(); }
}
