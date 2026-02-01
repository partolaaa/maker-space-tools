package com.makerspacetools.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Timing details for a booking request.
 */
record BookingTiming(
        LocalDate date,
        LocalTime startTime,
        int durationMinutes,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        Instant startInstant,
        Instant endInstant) {
}
