package com.makerspacetools.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token response payload from MakerSpace.
 */
public record MakerSpaceTokenResponse(
        @JsonProperty("access_token")
        @JsonAlias({"accessToken", "token"})
        String accessToken,
        @JsonProperty("token_type")
        @JsonAlias({"tokenType"})
        String tokenType,
        @JsonProperty("expires_in")
        @JsonAlias({"expiresIn"})
        Long expiresIn) {
}
