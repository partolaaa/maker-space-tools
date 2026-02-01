package com.makerspacetools.makerspace.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response wrapper for the bookings/my endpoint.
 *
 * @param myBookings list of bookings for the current user
 */
public record MakerSpaceMyBookingsResponse(
        @JsonProperty("MyBookings")
        List<MyBooking> myBookings) {

    /**
     * Booking entry with minimal fields used by the UI.
     */
    public record MyBooking(
            @JsonProperty("Id")
            long id,
            @JsonProperty("BookingNumber")
            Long bookingNumber,
            @JsonProperty("FromTime")
            LocalDateTime fromTime,
            @JsonProperty("ToTime")
            LocalDateTime toTime,
            @JsonProperty("CreatedOn")
            LocalDateTime createdOn,
            @JsonProperty("IsCancelled")
            Boolean isCancelled) {
    }
}
