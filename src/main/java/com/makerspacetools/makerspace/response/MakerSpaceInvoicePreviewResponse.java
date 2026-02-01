package com.makerspacetools.makerspace.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Preview response from the invoice API.
 */
public record MakerSpaceInvoicePreviewResponse(
        @JsonProperty("Errors")
        List<Error> errors,
        @JsonProperty("Message")
        String message,
        @JsonProperty("WasSuccessful")
        Boolean wasSuccessful) {

    /**
     * Error entry returned by the preview API.
     */
    public record Error(
            @JsonProperty("Message")
            String message,
            @JsonProperty("PropertyName")
            String propertyName) {
    }
}
