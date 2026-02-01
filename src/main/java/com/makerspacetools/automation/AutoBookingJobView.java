package com.makerspacetools.automation;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * View model for auto-booking jobs.
 */
@Builder
public record AutoBookingJobView(
        UUID id,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        DayOfWeek dayOfWeek,
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,
        AutoBookingJobStatus status,
        Instant lastAttemptAt,
        LocalDate lastBookedDate) {

    /**
     * Creates a view from a job definition.
     *
     * @param job job definition
     * @return view model
     */
    public static AutoBookingJobView from(AutoBookingJob job) {
        return AutoBookingJobView.builder()
                .id(job.id())
                .startDate(job.startDate())
                .dayOfWeek(job.dayOfWeek())
                .startTime(job.startTime())
                .endTime(job.endTime())
                .status(job.status())
                .lastAttemptAt(job.lastAttemptAt())
                .lastBookedDate(job.lastBookedDate())
                .build();
    }
}
