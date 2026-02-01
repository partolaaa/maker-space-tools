package com.makerspacetools.makerspace.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload for cancelling a booking.
 *
 * @param cancellationReason cancellation reason code
 * @param cancellationReasonDetails optional details
 */
public record MakerSpaceCancelBookingRequest(
        @JsonProperty("cancellationReason")
        String cancellationReason,
        @JsonProperty("cancellationReasonDetails")
        String cancellationReasonDetails) {
}
