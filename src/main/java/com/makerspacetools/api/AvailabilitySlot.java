package com.makerspacetools.api;

import com.makerspacetools.makerspace.response.MakerSpaceResourceAvailabilityResponse;

/**
 * Availability status for a single time slot.
 */
public record AvailabilitySlot(String time, boolean available, boolean booked) {

    public static AvailabilitySlot from(MakerSpaceResourceAvailabilityResponse.AvailableSlot slot) {
        return new AvailabilitySlot(slot.time(), slot.isFree(), slot.booked());
    }
}
