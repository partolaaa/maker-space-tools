package com.makerspacetools.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Authentication settings for MakerSpace API.
 *
 * @param username account username
 * @param password account password
 * @param clientId client identifier
 * @param totp totp code if required
 */
@ConfigurationProperties(prefix = "makerspace.auth")
record MakerSpaceAuthProperties(String username, String password, String clientId, String totp) {
}
