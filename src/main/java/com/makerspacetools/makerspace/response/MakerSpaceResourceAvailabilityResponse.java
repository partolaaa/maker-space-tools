package com.makerspacetools.makerspace.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Availability response for a resource.
 *
 * @param resource resource metadata
 * @param availableSlots available slots
 */
public record MakerSpaceResourceAvailabilityResponse(
        @JsonProperty("Resource")
        Resource resource,
        @JsonProperty("AvailableSlots")
        List<AvailableSlot> availableSlots) {

    /**
     * Resource metadata for availability response.
     */
    public record Resource(
            @JsonProperty("NoReturnPolicy")
            String noReturnPolicy,
            @JsonProperty("NoReturnPolicyAllUsers")
            Boolean noReturnPolicyAllUsers,
            @JsonProperty("NoReturnPolicyAllResources")
            Boolean noReturnPolicyAllResources,
            @JsonProperty("IntervalLimit")
            Integer intervalLimit,
            @JsonProperty("Name")
            String name,
            @JsonProperty("Id")
            long id) {
    }

    /**
     * Availability slot within a day.
     */
    public record AvailableSlot(
            @JsonProperty("DateTime")
            LocalDateTime dateTime,
            @JsonProperty("Date")
            LocalDateTime date,
            @JsonProperty("Time")
            String time,
            @JsonProperty("Available")
            boolean available,
            @JsonProperty("AllowMultipleBookings")
            boolean allowMultipleBookings,
            @JsonProperty("Capacity")
            int capacity,
            @JsonProperty("BookedCount")
            int bookedCount,
            @JsonProperty("BookedDesks")
            List<BookedDesk> bookedDesks,
            @JsonProperty("Booked")
            boolean booked) {

        public boolean isFree() {
            return available() && !booked();
        }
    }

    /**
     * Booked desk reference.
     *
     * @param id booked desk id
     */
    public record BookedDesk(Long id) {

        /**
         * Builds a BookedDesk from numeric or object payload.
         *
         * @param value payload value
         * @return booked desk reference
         */
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static BookedDesk fromValue(Object value) {
            switch (value) {
            case null -> {
                return new BookedDesk(null);
            }
            case Number number -> {
                return new BookedDesk(number.longValue());
            }
            case Map<?, ?> map -> {
                Object idValue = map.get("Id");
                if (idValue instanceof Number number) {
                    return new BookedDesk(number.longValue());
                }
                if (idValue instanceof String text) {
                    try {
                        return new BookedDesk(Long.parseLong(text));
                    } catch (NumberFormatException ignored) {
                        return new BookedDesk(null);
                    }
                }
            }
            default -> {
            }
            }
            return new BookedDesk(null);
        }
    }
}
