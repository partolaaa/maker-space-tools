package com.makerspacetools;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;

/**
 * Application entry point.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@Log4j2
public class MakerSpaceToolsApplication implements CommandLineRunner {

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
}
