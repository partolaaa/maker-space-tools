package com.makerspacetools.auth;

import lombok.Builder;

/**
 * Credentials used for MakerSpace authentication.
 */
@Builder
record MakerSpaceAuthCredentials(String username, String password, String clientId, String totp) {

    static MakerSpaceAuthCredentials from(AuthLoginRequest request) {
        return MakerSpaceAuthCredentials.builder()
                .username(request.username())
                .password(request.password())
                .clientId(request.clientId())
                .totp(request.totp())
                .build();
    }

    static MakerSpaceAuthCredentials from(MakerSpaceAuthProperties properties) {
        return MakerSpaceAuthCredentials.builder()
                .username(properties.username())
                .password(properties.password())
                .clientId(properties.clientId())
                .totp(properties.totp())
                .build();
    }
}
