package com.makerspacetools.controller;

import com.makerspacetools.automation.AutoBookingJobRequest;
import com.makerspacetools.automation.AutoBookingJobService;
import com.makerspacetools.automation.AutoBookingJobUpdateRequest;
import com.makerspacetools.automation.AutoBookingJobView;
import com.makerspacetools.automation.AutomationProperties;
import com.makerspacetools.automation.BookingAttempt;
import com.makerspacetools.automation.BookingAttemptLogger;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for auto-booking jobs and attempts.
 */
@RestController
@RequestMapping("/api/automation")
class AutomationController {

    private final AutoBookingJobService jobService;
    private final BookingAttemptLogger attemptLog;
    private final AutomationProperties properties;

    @Autowired
    AutomationController(
            AutoBookingJobService jobService,
            BookingAttemptLogger attemptLog,
            AutomationProperties properties) {
        this.jobService = jobService;
        this.attemptLog = attemptLog;
        this.properties = properties;
    }

    /**
     * Lists all auto-booking jobs.
     *
     * @return job views
     */
    @GetMapping("/jobs")
    List<AutoBookingJobView> listJobs() {
        return jobService.listJobs();
    }

    /**
     * Creates a new auto-booking job.
     *
     * @param request job request
     * @return created job view
     */
    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    AutoBookingJobView createJob(@RequestBody AutoBookingJobRequest request) {
        return jobService.createJob(request);
    }

    /**
     * Updates the status of a job.
     *
     * @param jobId job identifier
     * @param request update request
     * @return updated job view
     */
    @PatchMapping("/jobs/{jobId}")
    AutoBookingJobView updateJob(
            @PathVariable UUID jobId,
            @RequestBody @Valid AutoBookingJobUpdateRequest request) {
        return jobService.updateStatus(jobId, request.status());
    }

    /**
     * Deletes a job.
     *
     * @param jobId job identifier
     */
    @DeleteMapping("/jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteJob(@PathVariable UUID jobId) {
        jobService.deleteJob(jobId);
    }

    /**
     * Lists recent booking attempts.
     *
     * @param limit maximum entries to return
     * @return attempts
     */
    @GetMapping("/attempts")
    List<BookingAttempt> listAttempts(@RequestParam(defaultValue = "100") int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, properties.feedSize()));
        return attemptLog.list(cappedLimit);
    }
}
