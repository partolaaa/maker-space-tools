package com.makerspacetools.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Nexudus uses {@link Instant} as {@link LocalDateTime}, so we need to adapt...
 */
final class NexudusBookingTimeAdjuster {

    private NexudusBookingTimeAdjuster() {}

    static Instant adjust(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneOffset.UTC).toInstant();
    }
}
