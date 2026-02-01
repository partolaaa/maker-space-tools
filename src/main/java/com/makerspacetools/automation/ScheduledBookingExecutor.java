package com.makerspacetools.automation;

import com.makerspacetools.auth.MakerSpaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
class ScheduledBookingExecutor {

    private final AutoBookingJobService jobService;
    private final AutoBookingScheduler scheduler;
    private final MakerSpaceAuthService authService;

    @Autowired
    ScheduledBookingExecutor(
            AutoBookingJobService jobService,
            AutoBookingScheduler scheduler,
            MakerSpaceAuthService authService) {
        this.jobService = jobService;
        this.scheduler = scheduler;
        this.authService = authService;
    }

    /**
     * Executes auto-booking attempts on a fixed interval.
     */
    @Scheduled(fixedDelayString = "${automation.scheduler-delay:PT1M}")
    public void execute() {
        List<AutoBookingJob> jobs = jobService.listJobDefinitions();
        if (jobs.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime nowLocal = LocalDateTime.ofInstant(now, zoneId);
        for (AutoBookingJob job : jobs) {
            LocalDate targetDate = scheduler.resolveTargetDate(job, nowLocal);
            if (targetDate == null) {
                continue;
            }
            LocalDateTime startDateTime = LocalDateTime.of(targetDate, job.startTime());
            Instant startInstant = startDateTime.atZone(zoneId).toInstant();
            if (!scheduler.shouldAttemptJob(job, targetDate, now, startInstant)) {
                continue;
            }
            authService.runWithFallback(() -> scheduler.attemptJob(job, targetDate, now));
        }
    }
}
