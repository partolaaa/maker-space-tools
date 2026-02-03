package com.makerspacetools.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

/**
 * Time zone configuration for booking logic.
 *
 * @param timeZone time zone used for booking calculations
 */
@Validated
@ConfigurationProperties(prefix = "booking")
public record BookingTimeProperties(@NotNull ZoneId timeZone) {
}
