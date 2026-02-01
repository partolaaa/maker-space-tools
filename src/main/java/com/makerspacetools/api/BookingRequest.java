package com.makerspacetools.api;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Booking request payload from the UI.
 *
 * @param date booking date
 * @param startTime booking start time
 * @param durationMinutes booking duration in minutes
 */
public record BookingRequest(
        LocalDate date,
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        int durationMinutes) {
}
