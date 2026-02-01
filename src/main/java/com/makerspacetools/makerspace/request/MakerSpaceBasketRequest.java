package com.makerspacetools.makerspace.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Basket payload for booking invoice creation.
 *
 * @param basket basket items
 * @param discountCode discount code
 */
public record MakerSpaceBasketRequest(List<BasketItem> basket, String discountCode) {

    public static MakerSpaceBasketRequest of(MakerSpaceBasketRequest.BasketItem item) {
        return new MakerSpaceBasketRequest(List.of(item), null);
    }

    /**
     * Basket item payload.
     *
     * @param type item type
     * @param booking booking payload
     */
    public record BasketItem(
            @JsonProperty("Type")
            String type,
            @JsonProperty("Booking")
            Booking booking) {

        public static BasketItem of(Booking booking) {
            return new BasketItem("booking", booking);
        }
    }

    /**
     * Booking payload for basket item.
     */
    @Builder
    public record Booking(
            @JsonProperty("UniqueId")
            String uniqueId,
            @JsonProperty("FromTime")
            Instant fromTime,
            @JsonProperty("ToTime")
            Instant toTime,
            @JsonProperty("ResourceId")
            long resourceId,
            @JsonProperty("CoworkerId")
            long coworkerId) {

        /**
         * Normalizes booking identifiers.
         */
        public Booking {
            uniqueId = Objects.requireNonNullElseGet(uniqueId, UUID::randomUUID).toString();
        }
    }
}