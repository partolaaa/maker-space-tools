package com.makerspacetools.controller;

import com.makerspacetools.auth.AuthLoginRequest;
import com.makerspacetools.auth.AuthLoginResponse;
import com.makerspacetools.auth.AuthStatusResponse;
import com.makerspacetools.auth.MakerSpaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

/**
 * REST controller for authentication operations.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private static final String SESSION_TOKEN_KEY = "MAKERSPACE_TOKEN";

    private final MakerSpaceAuthService authService;

    @Autowired
    AuthController(MakerSpaceAuthService authService) {
        this.authService = authService;
    }

    /**
     * Logs in and stores runtime credentials.
     *
     * @param request login request
     * @param session http session
     * @return login response
     */
    @PostMapping("/login")
    AuthLoginResponse login(@RequestBody AuthLoginRequest request, HttpSession session) {
        String token = authService.login(request);
        if (session != null) {
            session.setAttribute(SESSION_TOKEN_KEY, token);
        }
        return AuthLoginResponse.success("Logged in.");
    }

    /**
     * Returns the current authentication status.
     *
     * @param session http session
     * @return status response
     */
    @GetMapping("/status")
    AuthStatusResponse status(HttpSession session) {
        boolean authenticated = session != null && session.getAttribute(SESSION_TOKEN_KEY) != null;
        return new AuthStatusResponse(authenticated);
    }

    /**
     * Logs out and clears cached credentials.
     *
     * @return logout response
     */
    @PostMapping("/logout")
    AuthLoginResponse logout(HttpSession session) {
        authService.logout();
        if (session != null) {
            session.removeAttribute(SESSION_TOKEN_KEY);
        }
        return AuthLoginResponse.success("Logged out.");
    }
}
