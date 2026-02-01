package com.makerspacetools.automation;

import com.makerspacetools.api.BookingRequest;
import com.makerspacetools.api.BookingResponse;
import com.makerspacetools.service.MachineBooker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled task that attempts auto-booking jobs.
 */
@Service
public class AutoBookingScheduler {

    private static final int WEEK_NUMBER_OF_DAYS = 7;
    private static final int MAX_BOOKING_HOURS_AHEAD = 360;

    private final AutoBookingJobService jobService;
    private final BookingAttemptLogger attemptLog;
    private final MachineBooker machineBooker;
    private final AutomationProperties properties;

    @Autowired
    AutoBookingScheduler(
            AutoBookingJobService jobService,
            BookingAttemptLogger attemptLog,
            MachineBooker machineBooker,
            AutomationProperties properties) {
        this.jobService = jobService;
        this.attemptLog = attemptLog;
        this.machineBooker = machineBooker;
        this.properties = properties;
    }

    LocalDate resolveTargetDate(AutoBookingJob job, LocalDateTime nowLocal) {
        LocalDate baseDate = nowLocal.toLocalDate();
        LocalTime baseTime = nowLocal.toLocalTime();
        LocalDate startDate = job.startDate();
        if (startDate != null && startDate.isAfter(baseDate)) {
            baseDate = startDate;
            baseTime = LocalTime.MIN;
        }
        return nextOccurrence(job, baseDate, baseTime);
    }

    boolean shouldAttemptJob(AutoBookingJob job, LocalDate targetDate, Instant now, Instant startInstant) {
        if (job.status() != AutoBookingJobStatus.ACTIVE) {
            return false;
        }
        if (job.lastBookedDate() != null && job.lastBookedDate().equals(targetDate)) {
            return false;
        }
        if (!isWithinBookingWindow(startInstant, now)) {
            return false;
        }
        return !shouldSkipAttempt(job, now);
    }

    private boolean isWithinBookingWindow(Instant startInstant, Instant now) {
        Instant maxAllowed = now.plus(MAX_BOOKING_HOURS_AHEAD, ChronoUnit.HOURS);
        return !startInstant.isBefore(now) && !startInstant.isAfter(maxAllowed);
    }

    void attemptJob(AutoBookingJob job, LocalDate targetDate, Instant now) {
        BookingRequest request = new BookingRequest(targetDate, job.startTime(), job.durationMinutes());
        BookingResponse response;
        try {
            response = machineBooker.bookForScheduledJob(request);
        } catch (Exception exception) {
            recordAttempt(job, targetDate, false, exception.getMessage());
            jobService.updateAfterAttempt(job.id(), now, null);
            return;
        }
        boolean success = response.success();
        String message = response.message();
        if (message == null || message.isBlank()) {
            message = success ? "Booking succeeded." : "Booking failed.";
        }
        recordAttempt(job, targetDate, success, message);
        jobService.updateAfterAttempt(job.id(), now, success ? targetDate : null);
    }

    private boolean shouldSkipAttempt(AutoBookingJob job, Instant now) {
        if (job.lastAttemptAt() == null) {
            return false;
        }
        Duration interval = properties.attemptInterval();
        return Duration.between(job.lastAttemptAt(), now).compareTo(interval) < 0;
    }

    private LocalDate nextOccurrence(AutoBookingJob job, LocalDate today, LocalTime nowTime) {
        int diff = job.dayOfWeek().getValue() - today.getDayOfWeek().getValue();
        if (diff < 0) {
            diff += WEEK_NUMBER_OF_DAYS;
        }
        LocalDate candidate = today.plusDays(diff);
        if (diff == 0 && !nowTime.isBefore(job.startTime())) {
            candidate = today.plusWeeks(1);
        }
        if (job.startDate() != null && candidate.isBefore(job.startDate())) {
            candidate = job.startDate();
        }
        return candidate;
    }

    private void recordAttempt(AutoBookingJob job, LocalDate targetDate, boolean success, String message) {
        BookingAttempt attempt = BookingAttempt.builder()
                .jobId(job.id())
                .targetDate(targetDate)
                .startTime(job.startTime())
                .endTime(job.endTime())
                .success(success)
                .message(message)
                .build();
        attemptLog.add(attempt);
    }
}
