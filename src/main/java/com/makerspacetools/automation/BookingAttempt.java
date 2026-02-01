package com.makerspacetools.automation;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Record of an auto-booking attempt.
 */
@Builder
public record BookingAttempt(
        UUID id,
        UUID jobId,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate targetDate,
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,
        boolean success,
        String message,
        Instant occurredAt) {

    /**
     * Ctor.
     */
    public BookingAttempt {
        id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
        occurredAt = Objects.requireNonNullElseGet(occurredAt, Instant::now);
    }
}
