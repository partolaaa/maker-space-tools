package com.makerspacetools.auth;

/**
 * Login request payload for MakerSpace authentication.
 */
public record AuthLoginRequest(String username, String password, String clientId, String totp) {
}
