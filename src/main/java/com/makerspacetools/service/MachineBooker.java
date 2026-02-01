package com.makerspacetools.service;

import com.makerspacetools.api.BookingRequest;
import com.makerspacetools.api.BookingResponse;
import com.makerspacetools.auth.MakerSpaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.UUID;

/**
 * Coordinates the booking flow.
 */
@Service
public class MachineBooker {
    private final BookingValidator validationService;
    private final BookingPreviewer previewService;
    private final BookingSubmitter submissionService;
    private final MakerSpaceAuthService authService;

    @Autowired
    MachineBooker(
            BookingValidator validationService,
            BookingPreviewer previewService,
            BookingSubmitter submissionService,
            MakerSpaceAuthService authService) {
        this.validationService = validationService;
        this.previewService = previewService;
        this.submissionService = submissionService;
        this.authService = authService;
    }

    /**
     * Books a machine slot after preview validation.
     *
     * @param request booking request
     * @return booking result
     */
    public BookingResponse book(BookingRequest request) {
        return bookManual(request);
    }

    /**
     * Books a machine slot for scheduled jobs with fallback retry.
     *
     * @param request booking request
     * @return booking result
     */
    public BookingResponse bookForScheduledJob(BookingRequest request) {
        return bookScheduled(request);
    }

    private BookingResponse bookManual(BookingRequest request) {
        try {
            return bookWithPreview(request);
        } catch (BookingValidationException exception) {
            return exception.response();
        } catch (RestClientResponseException exception) {
            return failureFromException(exception);
        }
    }

    private BookingResponse bookScheduled(BookingRequest request) {
        try {
            return bookWithPreview(request);
        } catch (BookingValidationException exception) {
            return exception.response();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() != HttpStatus.UNAUTHORIZED) {
                throw exception;
            }
            authService.forceFallbackNext();
            authService.invalidateToken();
            return bookWithPreview(request);
        }
    }

    private BookingResponse bookWithPreview(BookingRequest request) {
        BookingTiming timing = validationService.validate(request);
        String uniqueId = UUID.randomUUID().toString();
        BookingResponse previewError = previewService.validatePreview(timing, uniqueId);
        if (previewError != null) {
            return previewError;
        }
        submissionService.submitBooking(timing, uniqueId);
        return new BookingResponse(true, "Booking confirmed.", List.of());
    }

    private BookingResponse failureFromException(RestClientResponseException exception) {
        String message = exception.getResponseBodyAsString();
        if (message.isBlank()) {
            message = "Booking failed.";
        }
        return new BookingResponse(false, message, List.of());
    }
}
