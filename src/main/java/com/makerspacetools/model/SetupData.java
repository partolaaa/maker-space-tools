package com.makerspacetools.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Setup data configured via application properties.
 *
 * @param coworker coworker configuration
 * @param embroideryMachine machine configuration
 */
@ConfigurationProperties(prefix = "data")
public record SetupData(
        Coworker coworker,
        EmbroideryMachine embroideryMachine) {

    /**
     * Coworker configuration details.
     *
     * @param id coworker id
     */
    public record Coworker(Long id) {
    }

    /**
     * Embroidery machine configuration details.
     *
     * @param guid machine guid
     * @param id machine id
     */
    public record EmbroideryMachine(String guid, Long id) {
    }
}
