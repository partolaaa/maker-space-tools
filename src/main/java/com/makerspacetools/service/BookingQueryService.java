package com.makerspacetools.service;

import com.makerspacetools.api.PendingBookingsResponse;
import com.makerspacetools.client.MakerSpaceClient;
import com.makerspacetools.makerspace.response.MakerSpaceMyBookingsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Service for querying pending bookings.
 */
@Service
public class BookingQueryService {

    private static final int DEFAULT_DEPTH = 3;

    private final MakerSpaceClient client;

    @Autowired
    BookingQueryService(MakerSpaceClient client) {
        this.client = client;
    }

    /**
     * Loads pending bookings for the current user.
     *
     * @return pending bookings response
     */
    public PendingBookingsResponse pendingBookings() {
        MakerSpaceMyBookingsResponse response = client.myBookings(DEFAULT_DEPTH);
        if (response == null || response.myBookings() == null || response.myBookings().isEmpty()) {
            return PendingBookingsResponse.empty();
        }
        LocalDateTime now = LocalDateTime.now();
        List<PendingBookingsResponse.BookingSummary> summaries = response.myBookings().stream()
                .filter(booking -> !Boolean.TRUE.equals(booking.isCancelled()))
                .filter(booking -> booking.toTime() == null || booking.toTime().isAfter(now))
                .sorted(Comparator.comparing(MakerSpaceMyBookingsResponse.MyBooking::fromTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(PendingBookingsResponse.BookingSummary::of)
                .toList();
        return PendingBookingsResponse.of(summaries);
    }
}
