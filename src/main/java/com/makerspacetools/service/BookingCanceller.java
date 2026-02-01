package com.makerspacetools.service;

import com.makerspacetools.client.MakerSpaceClient;
import com.makerspacetools.makerspace.request.MakerSpaceCancelBookingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for cancelling bookings.
 */
@Service
public class BookingCanceller {

    private final MakerSpaceClient client;

    @Autowired
    BookingCanceller(MakerSpaceClient client) {
        this.client = client;
    }

    /**
     * Cancels a booking by id.
     *
     * @param bookingId booking id
     */
    public void cancelBooking(long bookingId) {
        MakerSpaceCancelBookingRequest request = new MakerSpaceCancelBookingRequest("NoLongerNeeded", null);
        client.cancelBooking(bookingId, request);
    }
}
