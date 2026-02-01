package com.makerspacetools.api;

import com.makerspacetools.makerspace.response.MakerSpaceMyBookingsResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload for pending bookings.
 *
 * @param bookings list of pending booking summaries
 */
public record PendingBookingsResponse(List<BookingSummary> bookings) {


    public static PendingBookingsResponse of(List<PendingBookingsResponse.BookingSummary> summaries) {
        return new PendingBookingsResponse(summaries);
    }

    public static PendingBookingsResponse empty() {
        return new PendingBookingsResponse(List.of());
    }

    /**
     * Summary view of a booking.
     */
    public record BookingSummary(
            long id,
            Long bookingNumber,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            LocalDateTime createdOn) {

        /**
         * Factory method to create {@link BookingSummary} from {@link MakerSpaceMyBookingsResponse.MyBooking}
         */
        public static BookingSummary of(MakerSpaceMyBookingsResponse.MyBooking booking) {
            return new BookingSummary(
                    booking.id(),
                    booking.bookingNumber(),
                    booking.fromTime(),
                    booking.toTime(),
                    booking.createdOn()
            );
        }
    }
}
