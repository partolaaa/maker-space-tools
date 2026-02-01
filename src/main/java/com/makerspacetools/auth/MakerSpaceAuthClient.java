package com.makerspacetools.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * HTTP client for MakerSpace authentication endpoints.
 */
@HttpExchange
public interface MakerSpaceAuthClient {

    /**
     * Requests a token from the authentication endpoint.
     *
     * @param form request body
     * @param clientId client identifier header
     * @return token response with headers
     */
    @PostExchange(value = "/api/token", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ResponseEntity<MakerSpaceTokenResponse> requestToken(
            @RequestBody MultiValueMap<String, String> form,
            @RequestHeader("client_id") String clientId);

    /**
     * Logs out the current session.
     *
     * @param authorization authorization header
     */
    @GetExchange("/en/login/logout")
    void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
}
