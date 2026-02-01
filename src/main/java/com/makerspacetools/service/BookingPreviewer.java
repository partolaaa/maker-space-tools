package com.makerspacetools.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.makerspacetools.api.BookingResponse;
import com.makerspacetools.client.MakerSpaceClient;
import com.makerspacetools.makerspace.request.MakerSpaceInvoicePreviewRequestItem;
import com.makerspacetools.makerspace.response.MakerSpaceInvoicePreviewResponse;
import com.makerspacetools.model.SetupData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Set;

/**
 * Service for previewing and validating invoices.
 */
@Service
class BookingPreviewer {

    private final MakerSpaceClient client;
    private final ObjectMapper objectMapper;
    private final SetupData setupData;

    @Autowired
    BookingPreviewer(MakerSpaceClient client, ObjectMapper objectMapper, SetupData setupData) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.setupData = setupData;
    }

    BookingResponse validatePreview(BookingTiming timing, String uniqueId) {
        MakerSpaceInvoicePreviewRequestItem previewItem = buildPreviewItem(uniqueId, timing);
        MakerSpaceInvoicePreviewResponse previewResponse = previewInvoice(Set.of(previewItem));
        return validatePreviewResponse(previewResponse);
    }

    private MakerSpaceInvoicePreviewRequestItem buildPreviewItem(String uniqueId, BookingTiming timing) {
        MakerSpaceInvoicePreviewRequestItem.Booking booking = MakerSpaceInvoicePreviewRequestItem.Booking.builder()
                .resourceId(setupData.embroideryMachine().id())
                .fromTime(timing.startInstant())
                .toTime(timing.endInstant())
                .coworkerId(setupData.coworker().id())
                .chargeNow(true)
                .uniqueId(uniqueId)
                .build();
        return MakerSpaceInvoicePreviewRequestItem.of(booking);
    }

    private MakerSpaceInvoicePreviewResponse previewInvoice(Set<MakerSpaceInvoicePreviewRequestItem> items) {
        try {
            return client.previewInvoice(items);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw exception;
            }
            String body = exception.getResponseBodyAsString();
            if (body.isBlank()) {
                return null;
            }
            try {
                return objectMapper.readValue(body, MakerSpaceInvoicePreviewResponse.class);
            } catch (Exception readException) {
                return null;
            }
        }
    }

    private BookingResponse validatePreviewResponse(MakerSpaceInvoicePreviewResponse previewResponse) {
        if (previewResponse == null) {
            return failureResponse("Booking preview failed.", List.of("Unable to validate the booking."));
        }
        List<String> previewErrors = extractPreviewErrors(previewResponse);
        if (!previewErrors.isEmpty()) {
            String message = previewResponse.message();
            if (message == null || message.isBlank()) {
                message = "Booking is not available.";
            }
            return failureResponse(message, previewErrors);
        }
        if (Boolean.FALSE.equals(previewResponse.wasSuccessful())) {
            String message = previewResponse.message();
            if (message == null || message.isBlank()) {
                message = "Booking preview failed.";
            }
            return failureResponse(message);
        }
        return null;
    }

    private List<String> extractPreviewErrors(MakerSpaceInvoicePreviewResponse previewResponse) {
        if (previewResponse.errors() == null || previewResponse.errors().isEmpty()) {
            return List.of();
        }
        return previewResponse.errors().stream()
                .map(this::resolveErrorMessage)
                .filter(message -> message != null && !message.isBlank())
                .toList();
    }

    private String resolveErrorMessage(MakerSpaceInvoicePreviewResponse.Error error) {
        if (error == null) {
            return null;
        }
        if (error.message() != null && !error.message().isBlank()) {
            return error.message();
        }
        if (error.propertyName() != null && !error.propertyName().isBlank()) {
            return error.propertyName();
        }
        return null;
    }

    private BookingResponse failureResponse(String message, List<String> errors) {
        return new BookingResponse(false, message, errors);
    }

    private BookingResponse failureResponse(String message) {
        return new BookingResponse(false, message, List.of());
    }
}
