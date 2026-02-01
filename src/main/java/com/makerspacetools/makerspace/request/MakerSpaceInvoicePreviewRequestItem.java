package com.makerspacetools.makerspace.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;

/**
 * Invoice preview request item.
 *
 * @param type item type
 * @param booking booking payload
 */
public record MakerSpaceInvoicePreviewRequestItem(
        @JsonProperty("Type")
        String type,
        @JsonProperty("Booking")
        Booking booking) {

    public static MakerSpaceInvoicePreviewRequestItem of(Booking booking) {
        return new MakerSpaceInvoicePreviewRequestItem("booking", booking);
    }

    /**
     * Booking payload for invoice preview.
     */
    @Builder
    public record Booking(
            @JsonProperty("Id")
            long id,
            @JsonProperty("ResourceId")
            long resourceId,
            @JsonProperty("FromTime")
            Instant fromTime,
            @JsonProperty("ToTime")
            Instant toTime,
            @JsonProperty("CoworkerId")
            long coworkerId,
            @JsonProperty("ChargeNow")
            boolean chargeNow,
            @JsonProperty("UniqueId")
            String uniqueId) {

    }
}
