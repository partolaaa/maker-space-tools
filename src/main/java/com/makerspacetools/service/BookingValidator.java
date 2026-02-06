package com.makerspacetools.service;

import com.makerspacetools.api.BookingRequest;
import com.makerspacetools.api.BookingResponse;
import com.makerspacetools.makerspace.response.MakerSpaceResourceAvailabilityResponse;
import com.makerspacetools.model.WorkDaySchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Validates booking requests and builds booking timing.
 */
@Service
class BookingValidator {

    private static final int MAX_BOOKING_DURATION_MINUTES = 240;

    private final MachineAvailabilityChecker availabilityService;

    @Autowired
    BookingValidator(MachineAvailabilityChecker availabilityService) {
        this.availabilityService = availabilityService;
    }

    BookingTiming validate(BookingRequest request) {
        BookingResponse error = validateBookingRequest(request);
        if (error != null) {
            throw new BookingValidationException(error);
        }
        BookingTiming timing = resolveTiming(request);
        BookingResponse windowError = validateBookingWindow(timing.startDateTime(), timing.endDateTime());
        if (windowError != null) {
            throw new BookingValidationException(windowError);
        }
        BookingResponse horizonError = validateBookingHorizon(timing.startInstant());
        if (horizonError != null) {
            throw new BookingValidationException(horizonError);
        }
        AvailabilityContext slotContext = availabilityService.availabilityContext(timing.date());
        BookingResponse intervalError = validateInterval(timing.durationMinutes(), slotContext.intervalMinutes());
        if (intervalError != null) {
            throw new BookingValidationException(intervalError);
        }
        BookingResponse availabilityError = validateSlotAvailability(
                slotContext.slots(),
                timing.startTime(),
                timing.durationMinutes(),
                slotContext.intervalMinutes());
        if (availabilityError != null) {
            throw new BookingValidationException(availabilityError);
        }
        return timing;
    }

    private BookingTiming resolveTiming(BookingRequest request) {
        LocalDate date = request.date();
        LocalTime startTime = request.startTime();
        int durationMinutes = request.durationMinutes();

        MachineAvailabilityChecker.validateDate(date);
        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        LocalDateTime endDateTime = startDateTime.plusMinutes(durationMinutes);
        ZoneId zoneId = ZoneId.systemDefault();
        Instant startInstant = startDateTime.atZone(zoneId).toInstant();
        Instant endInstant = endDateTime.atZone(zoneId).toInstant();

        return new BookingTiming(date, startTime, durationMinutes, startDateTime, endDateTime, startInstant, endInstant);
    }

    private static BookingResponse validateBookingRequest(BookingRequest request) {
        if (request == null || request.date() == null || request.startTime() == null) {
            return failureResponse("Missing booking details.", List.of("Date and start time are required."));
        }
        int durationMinutes = request.durationMinutes();
        if (durationMinutes <= 0) {
            return failureResponse("Invalid booking duration.", List.of("Duration must be positive."));
        }
        if (durationMinutes > MAX_BOOKING_DURATION_MINUTES) {
            return failureResponse("Booking is too long.", List.of("Maximum booking duration is 4 hours."));
        }
        return null;
    }

    private static BookingResponse validateBookingWindow(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (!startDateTime.toLocalDate().equals(endDateTime.toLocalDate())) {
            return failureResponse("Booking must stay within a single day.", List.of("Choose a shorter duration."));
        }
        LocalDate date = startDateTime.toLocalDate();
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return failureResponse("Bookings are not available on Sundays.", List.of("Pick a weekday or Saturday."));
        }
        WorkDaySchedule.TimeWindow workday = WorkDaySchedule.businessHours().windowFor(date.getDayOfWeek());
        if (startDateTime.toLocalTime().isBefore(workday.start()) || endDateTime.toLocalTime().isAfter(workday.end())) {
            return failureResponse("Booking is outside working hours.", List.of("Working hours are 08:00-16:00 Mon-Fri and 09:00-17:00 Sat."));
        }
        return null;
    }

    private static BookingResponse validateBookingHorizon(Instant startInstant) {
        Instant maxAllowed = MachineAvailabilityChecker.maxAllowedInstant();
        if (startInstant.isAfter(maxAllowed)) {
            return failureResponse("Booking is too far in the future.", List.of("Maximum is 360 hours ahead."));
        }
        return null;
    }

    private static BookingResponse validateInterval(int durationMinutes, int intervalMinutes) {
        if (durationMinutes < intervalMinutes || durationMinutes % intervalMinutes != 0) {
            return failureResponse("Duration must align with slot intervals.", List.of("Use increments of " + intervalMinutes + " minutes."));
        }
        return null;
    }

    private static BookingResponse validateSlotAvailability(
            List<MakerSpaceResourceAvailabilityResponse.AvailableSlot> slotsForDate,
            LocalTime startTime,
            int durationMinutes,
            int intervalMinutes) {
        if (!isSlotRangeAvailable(slotsForDate, startTime, durationMinutes, intervalMinutes)) {
            return failureResponse("Selected time is not available.", List.of("Pick a different start time."));
        }
        return null;
    }

    private static boolean isSlotRangeAvailable(
            List<MakerSpaceResourceAvailabilityResponse.AvailableSlot> slots,
            LocalTime startTime,
            int durationMinutes,
            int intervalMinutes) {
        Map<LocalTime, MakerSpaceResourceAvailabilityResponse.AvailableSlot> slotMap = new TreeMap<>();
        for (MakerSpaceResourceAvailabilityResponse.AvailableSlot slot : slots) {
            if (slot.dateTime() != null) {
                slotMap.put(slot.dateTime().toLocalTime(), slot);
            }
        }
        int requiredSlots = durationMinutes / intervalMinutes;
        for (int index = 0; index < requiredSlots; index++) {
            LocalTime time = startTime.plusMinutes((long) index * intervalMinutes);
            MakerSpaceResourceAvailabilityResponse.AvailableSlot slot = slotMap.get(time);
            if (slot == null || slot.booked() || !slot.available()) {
                return false;
            }
        }
        return true;
    }

    private static BookingResponse failureResponse(String message, List<String> errors) {
        return new BookingResponse(false, message, errors);
    }
}
