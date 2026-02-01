package com.makerspacetools.auth;

import org.springframework.http.HttpStatus;

/**
 * Exception with an authentication response status.
 */
public class AuthResponseException extends RuntimeException {

    private final HttpStatus status;

    AuthResponseException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
