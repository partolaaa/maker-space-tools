package com.makerspacetools.service;

import com.makerspacetools.api.AvailabilitySlot;
import com.makerspacetools.api.MachineAvailabilityResponse;
import com.makerspacetools.makerspace.response.MakerSpaceResourceAvailabilityResponse;
import com.makerspacetools.model.SetupData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Service for machine availability queries.
 */
@Service
public class MachineAvailabilityChecker {

    private static final int DEFAULT_INTERVAL_MINUTES = 30;
    private static final int MAX_BOOKING_HOURS_AHEAD = 360;
    private static final LocalTime WORKDAY_START = LocalTime.of(9, 0);
    private static final LocalTime WORKDAY_END = LocalTime.of(17, 0);
    private static final String DEFAULT_MACHINE_NAME = "Embroidery Machine";

    private final MachineQueryService queryService;
    private final SetupData setupData;

    @Autowired
    MachineAvailabilityChecker(MachineQueryService queryService, SetupData setupData) {
        this.queryService = queryService;
        this.setupData = setupData;
    }

    /**
     * Loads availability for the given date.
     *
     * @param date date to query
     * @return availability response for the date
     */
    public MachineAvailabilityResponse availabilityFor(LocalDate date) {
        validateDate(date);
        MakerSpaceResourceAvailabilityResponse availability = loadAvailability(date);
        List<MakerSpaceResourceAvailabilityResponse.AvailableSlot> slotsForDate = slotsForDate(availability, date).stream()
                .filter(slot -> isWithinWorkday(slot.dateTime()))
                .toList();
        List<AvailabilitySlot> slots = slotsForDate.stream()
                .map(AvailabilitySlot::from)
                .toList();
        String resourceName = availability.resource() == null ? DEFAULT_MACHINE_NAME : availability.resource().name();
        return new MachineAvailabilityResponse(resourceName, date, slots);
    }

    AvailabilityContext availabilityContext(LocalDate date) {
        MakerSpaceResourceAvailabilityResponse availability = loadAvailability(date);
        List<MakerSpaceResourceAvailabilityResponse.AvailableSlot> slotsForDate = slotsForDate(availability, date);
        int intervalMinutes = resolveIntervalMinutes(slotsForDate);
        return new AvailabilityContext(slotsForDate, intervalMinutes);
    }

    static void validateDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date must be today or later.");
        }
        Instant maxAllowed = maxAllowedInstant();
        LocalDate maxDate = LocalDateTime.ofInstant(maxAllowed, ZoneId.systemDefault()).toLocalDate();
        if (date.isAfter(maxDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date is more than %d hours in the future."
                    .formatted(MAX_BOOKING_HOURS_AHEAD));
        }
    }

    static Instant maxAllowedInstant() {
        return Instant.now().plus(MAX_BOOKING_HOURS_AHEAD, ChronoUnit.HOURS);
    }

    private MakerSpaceResourceAvailabilityResponse loadAvailability(LocalDate date) {
        String startTime = LocalDateTime.of(date, LocalTime.MIDNIGHT).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String guid = setupData.embroideryMachine().guid();
        return queryService.checkAvailability(1, guid, startTime, DEFAULT_INTERVAL_MINUTES);
    }

    private List<MakerSpaceResourceAvailabilityResponse.AvailableSlot> slotsForDate(
            MakerSpaceResourceAvailabilityResponse availability,
            LocalDate date) {
        if (availability == null || availability.availableSlots() == null) {
            return List.of();
        }
        return availability.availableSlots().stream()
                .filter(slot -> slot.dateTime() != null && Objects.equals(slot.dateTime().toLocalDate(), date))
                .sorted(Comparator.comparing(MakerSpaceResourceAvailabilityResponse.AvailableSlot::dateTime))
                .toList();
    }

    private static int resolveIntervalMinutes(List<MakerSpaceResourceAvailabilityResponse.AvailableSlot> slots) {
        if (slots.size() < 2) {
            return DEFAULT_INTERVAL_MINUTES;
        }
        LocalDateTime first = slots.getFirst().dateTime();
        LocalDateTime second = slots.get(1).dateTime();
        if (first == null || second == null) {
            return DEFAULT_INTERVAL_MINUTES;
        }
        long minutes = Duration.between(first, second).toMinutes();
        return minutes <= 0 ? DEFAULT_INTERVAL_MINUTES : (int) minutes;
    }

    private boolean isWithinWorkday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        LocalTime time = dateTime.toLocalTime();
        return !time.isBefore(WORKDAY_START) && time.isBefore(WORKDAY_END);
    }
}
