package com.makerspacetools.automation;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating auto-booking job settings.
 *
 * @param status job status
 */
public record AutoBookingJobUpdateRequest(@NotNull AutoBookingJobStatus status) {
}
