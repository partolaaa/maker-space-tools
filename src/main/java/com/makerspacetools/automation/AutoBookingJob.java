package com.makerspacetools.automation;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Scheduled auto-booking job definition.
 */
@Builder(toBuilder = true)
public record AutoBookingJob(
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
        LocalDate lastBookedDate,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Ctor.
     */
    public AutoBookingJob {
        id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
        createdAt = Objects.requireNonNullElseGet(createdAt, Instant::now);
        updatedAt = createdAt;
        status = Objects.requireNonNullElse(status, AutoBookingJobStatus.ACTIVE);
    }

    /**
     * Creates a job definition from a request.
     *
     * @param request creation request
     * @return job definition
     */
    public static AutoBookingJob from(AutoBookingJobRequest request) {
        return AutoBookingJob.builder()
                .startDate(request.startDate())
                .dayOfWeek(request.startDate().getDayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(request.status())
                .build();
    }

    /**
     * Returns a copy with updated status.
     */
    public AutoBookingJob withStatus(AutoBookingJobStatus status) {
        return this.toBuilder()
                .status(status)
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Returns a copy with updated attempt timestamp.
     */
    public AutoBookingJob withLastAttemptAt(Instant lastAttemptAt) {
        return this.toBuilder()
                .lastBookedDate(lastBookedDate)
                .lastAttemptAt(lastAttemptAt)
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Returns a copy with updated booking date.
     */
    public AutoBookingJob withLastBookedDate(LocalDate lastBookedDate) {
        return this.toBuilder()
                .lastBookedDate(lastBookedDate)
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Returns the duration of the booking in minutes.
     */
    public int durationMinutes() {
        return (int) Duration.between(startTime, endTime).toMinutes();
    }
}
