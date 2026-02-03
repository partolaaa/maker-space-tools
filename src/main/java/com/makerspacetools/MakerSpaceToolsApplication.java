package com.makerspacetools;

import com.makerspacetools.config.BookingTimeProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;
import java.util.TimeZone;

/**
 * Application entry point.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@Log4j2
public class MakerSpaceToolsApplication implements CommandLineRunner {

    private final BookingTimeProperties bookingTimeProperties;

    @Autowired
    MakerSpaceToolsApplication(BookingTimeProperties bookingTimeProperties) {
        this.bookingTimeProperties = bookingTimeProperties;
    }

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MakerSpaceToolsApplication.class, args);
    }

    /**
     * Runs on application startup.
     *
     * @param args startup arguments
     */
    @Override
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now();
        log.info("Application started at {}...", now);
    }

    @PostConstruct
    public void executeAfterMain() {
        TimeZone.setDefault(TimeZone.getTimeZone(bookingTimeProperties.timeZone()));
        log.info("Default time zone set to {}", bookingTimeProperties.timeZone());
    }
}
