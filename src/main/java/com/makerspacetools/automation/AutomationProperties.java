package com.makerspacetools.automation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration settings for auto-booking automation.
 *
 * @param jobsFile file path for storing scheduled jobs
 * @param schedulerDelay delay between scheduler runs
 * @param attemptInterval minimum time between job attempts
 * @param feedSize maximum entries retained in the attempt feed
 */
@ConfigurationProperties(prefix = "automation")
public record AutomationProperties(Path jobsFile, Duration schedulerDelay, Duration attemptInterval, int feedSize) {

    /**
     * Normalizes configuration defaults.
     */
    public AutomationProperties {
        jobsFile = Objects.requireNonNullElse(jobsFile, Path.of("data/auto-booking-jobs.json"));
        schedulerDelay = Objects.requireNonNullElse(schedulerDelay, Duration.ofMinutes(1));
        attemptInterval = Objects.requireNonNullElse(attemptInterval, Duration.ofMinutes(5));
        if (feedSize <= 0) {
            feedSize = 200;
        }
    }
}
