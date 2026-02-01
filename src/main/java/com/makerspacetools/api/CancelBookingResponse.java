package com.makerspacetools.api;

/**
 * Response payload for booking cancellation.
 *
 * @param success whether cancellation succeeded
 * @param message response message
 */
public record CancelBookingResponse(boolean success, String message) {

    public static final CancelBookingResponse SUCCESS = new CancelBookingResponse(true, "Booking cancelled successfully.");
}
