package com.makerspacetools.controller;

import com.makerspacetools.auth.AuthLoginResponse;
import com.makerspacetools.auth.AuthResponseException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles authentication exceptions for the auth controller.
 */
@RestControllerAdvice(assignableTypes = AuthController.class)
class AuthExceptionHandler {

    @ExceptionHandler(AuthResponseException.class)
    AuthLoginResponse handleAuthResponse(AuthResponseException exception, HttpServletResponse response) {
        response.setStatus(exception.status().value());
        return AuthLoginResponse.failed(exception.getMessage());
    }
}
