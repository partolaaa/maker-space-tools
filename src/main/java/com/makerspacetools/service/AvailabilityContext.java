package com.makerspacetools.service;

import com.makerspacetools.makerspace.response.MakerSpaceResourceAvailabilityResponse;

import java.util.List;

/**
 * Availability slots and interval metadata.
 *
 * @param slots available slots for the day
 * @param intervalMinutes interval between slots
 */
record AvailabilityContext(
        List<MakerSpaceResourceAvailabilityResponse.AvailableSlot> slots,
        int intervalMinutes) {
}
