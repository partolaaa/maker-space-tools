package com.makerspacetools.api;

import java.util.List;

/**
 * Response from booking attempt.
 *
 * @param success whether the booking succeeded
 * @param message user-facing message
 * @param errors optional error details
 */
public record BookingResponse(
        boolean success,
        String message,
        List<String> errors) {
}
