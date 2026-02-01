package com.makerspacetools.controller;

import com.makerspacetools.api.CancelBookingResponse;
import com.makerspacetools.api.PendingBookingsResponse;
import com.makerspacetools.service.BookingCanceller;
import com.makerspacetools.service.BookingQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for booking queries.
 */
@RestController
@RequestMapping("/api/bookings")
class BookingController {

    private final BookingCanceller bookingCanceller;
    private final BookingQueryService bookingQueryService;

    /**
     * Creates a new booking controller.
     *
     * @param bookingQueryService booking query service
     */
    BookingController(BookingCanceller bookingCanceller, BookingQueryService bookingQueryService) {
        this.bookingCanceller = bookingCanceller;
        this.bookingQueryService = bookingQueryService;
    }

    /**
     * Returns pending bookings for the current user.
     *
     * @return pending bookings response
     */
    @GetMapping("/pending")
    PendingBookingsResponse pendingBookings() {
        return bookingQueryService.pendingBookings();
    }

    /**
     * Cancels a booking by id.
     *
     * @param bookingId booking id
     * @return cancellation response
     */
    @PostMapping("/cancel/{bookingId}")
    CancelBookingResponse cancelBooking(@PathVariable long bookingId) {
        bookingCanceller.cancelBooking(bookingId);
        return CancelBookingResponse.SUCCESS;
    }
}
