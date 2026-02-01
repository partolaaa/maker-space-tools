package com.makerspacetools.api;

import java.time.LocalDate;
import java.util.List;

/**
 * Availability for a machine on a given date.
 *
 * @param resourceName machine name
 * @param date date for the returned slots
 * @param slots availability slots for the date
 */
public record MachineAvailabilityResponse(
        String resourceName,
        LocalDate date,
        List<AvailabilitySlot> slots) {
}
