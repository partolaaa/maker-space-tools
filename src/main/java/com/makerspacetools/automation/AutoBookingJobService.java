package com.makerspacetools.automation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing auto-booking jobs.
 */
@Service
public class AutoBookingJobService {

    private static final LocalTime WORKDAY_START = LocalTime.of(9, 0);
    private static final LocalTime WORKDAY_END = LocalTime.of(17, 0);
    private static final int MAX_DURATION_MINUTES = 240;
    private static final int SLOT_MINUTES = 30;

    private final AutoBookingJobStorageService storageService;

    @Autowired
    AutoBookingJobService(AutoBookingJobStorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Returns all jobs for display.
     *
     * @return job views
     */
    public List<AutoBookingJobView> listJobs() {
        return storageService.list().stream().map(AutoBookingJobView::from).toList();
    }

    List<AutoBookingJob> listJobDefinitions() {
        return storageService.list();
    }

    /**
     * Creates a new auto-booking job.
     *
     * @param request creation request
     * @return created job view
     */
     public AutoBookingJobView createJob(AutoBookingJobRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        validate(request);
        AutoBookingJob job = AutoBookingJob.from(request);
        return AutoBookingJobView.from(storageService.add(job));
    }

    /**
     * Updates the status of a job.
     *
     * @param jobId job identifier
     * @param status new job status
     * @return updated job view
     */
    public AutoBookingJobView updateStatus(UUID jobId, AutoBookingJobStatus status) {
        AutoBookingJob job = storageService.find(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));
        AutoBookingJob updated = job.withStatus(status);
        return AutoBookingJobView.from(storageService.update(updated)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found.")));
    }

    /**
     * Deletes a job by identifier.
     *
     * @param jobId job identifier
     */
    public void deleteJob(UUID jobId) {
        boolean removed = storageService.delete(jobId);
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found.");
        }
    }

    void updateAfterAttempt(UUID jobId, Instant attemptAt, LocalDate bookedDate) {
        AutoBookingJob job = storageService.find(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found."));
        AutoBookingJob updated = job.withLastAttemptAt(attemptAt);
        AutoBookingJob resolved = bookedDate == null ? updated : updated.withLastBookedDate(bookedDate);
        storageService.update(resolved);
    }

    private void validate(AutoBookingJobRequest request) {
        LocalDate startDate = request.startDate();
        LocalTime startTime = request.startTime();
        LocalTime endTime = validatedEndTime(request, startDate, startTime);
        int durationMinutes = (int) Duration.between(startTime, endTime).toMinutes();
        if (durationMinutes > MAX_DURATION_MINUTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum booking duration is 4 hours.");
        }
        if (durationMinutes % SLOT_MINUTES != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Times must align to 30-minute slots.");
        }
    }

    private static LocalTime validatedEndTime(AutoBookingJobRequest request, LocalDate startDate, LocalTime startTime) {
        LocalTime endTime = request.endTime();
        if (startDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date is required.");
        }
        if (startTime == null || endTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start and end time are required.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time.");
        }
        if (startTime.isBefore(WORKDAY_START) || endTime.isAfter(WORKDAY_END)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time must be within 09:00 and 17:00.");
        }
        return endTime;
    }
}
