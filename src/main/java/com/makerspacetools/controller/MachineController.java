package com.makerspacetools.controller;

import com.makerspacetools.api.BookingRequest;
import com.makerspacetools.api.BookingResponse;
import com.makerspacetools.api.MachineAvailabilityResponse;
import com.makerspacetools.service.MachineAvailabilityChecker;
import com.makerspacetools.service.MachineBooker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST endpoints for machine availability and booking.
 */
@RestController
@RequestMapping("/api/machines")
class MachineController {

    private final MachineBooker machineBooker;
    private final MachineAvailabilityChecker availabilityService;

    @Autowired
    MachineController(MachineBooker machineBooker, MachineAvailabilityChecker availabilityService) {
        this.machineBooker = machineBooker;
        this.availabilityService = availabilityService;
    }

    /**
     * Returns availability for the requested date.
     *
     * @param date selected date
     * @return availability response
     */
    @GetMapping("/availability")
    MachineAvailabilityResponse availability(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return availabilityService.availabilityFor(date);
    }

    /**
     * Books a machine slot after preview validation.
     *
     * @param request booking request
     * @return booking result
     */
    @PostMapping("/bookings")
    BookingResponse book(@RequestBody BookingRequest request) {
        return machineBooker.book(request);
    }
}
