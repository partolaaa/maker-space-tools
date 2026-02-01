package com.makerspacetools.auth;

/**
 * Login response payload for MakerSpace authentication.
 *
 * @param success whether authentication succeeded
 * @param message response message
 */
public record AuthLoginResponse(boolean success, String message) {

    public static AuthLoginResponse success(String message) {
        return new AuthLoginResponse(true, message);
    }

    public static AuthLoginResponse failed(String message) {
        return new AuthLoginResponse(false, message);
    }
}
