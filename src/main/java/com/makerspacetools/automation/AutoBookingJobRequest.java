package com.makerspacetools.automation;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Request payload for creating an auto-booking job.
 */
public record AutoBookingJobRequest(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,
        AutoBookingJobStatus status) {

    /**
     * Ctor.
     */
    public AutoBookingJobRequest {
        Objects.requireNonNull(startDate, "Start date is required.");
        Objects.requireNonNull(startTime, "Start time is required.");
        Objects.requireNonNull(endTime, "End time is required.");

        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
    }
}
