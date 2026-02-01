package com.makerspacetools.service;

import com.makerspacetools.api.BookingResponse;

/**
 * Exception used for booking validation responses.
 */
class BookingValidationException extends RuntimeException {

    private final BookingResponse response;

    BookingValidationException(BookingResponse response) {
        super(response.message());
        this.response = response;
    }

    BookingResponse response() {
        return response;
    }
}
