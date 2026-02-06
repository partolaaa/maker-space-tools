package com.makerspacetools.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles authentication and token refresh for MakerSpace API.
 */
@Log4j2
@Service
public class MakerSpaceAuthService {

    private static final Duration DEFAULT_TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration REFRESH_BUFFER = Duration.ofMinutes(1);

    private final MakerSpaceAuthProperties properties;
    private final MakerSpaceAuthClient authClient;
    private final ObjectMapper objectMapper;
    private final Object lock;
    private volatile TokenState tokenState;
    private volatile MakerSpaceAuthCredentials runtimeCredentials;
    private final AtomicBoolean useFallbackNext;
    private final ThreadLocal<Boolean> fallbackAllowed;

    @Autowired
    MakerSpaceAuthService(
            MakerSpaceAuthProperties properties,
            MakerSpaceAuthClient authClient,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.authClient = authClient;
        this.objectMapper = objectMapper;
        this.lock = new Object();
        this.useFallbackNext = new AtomicBoolean(false);
        this.fallbackAllowed = ThreadLocal.withInitial(() -> Boolean.FALSE);
    }

    /**
     * Returns the current access token, refreshing if needed.
     *
     * @return access token
     */
    public String getAccessToken() {
        boolean fallbackEnabled = isFallbackAllowed();
        ResolvedCredentials resolved = resolveCredentials(fallbackEnabled);
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credentials are missing.");
        }
        TokenState state = tokenState;
        if (state == null || state.isExpired() || state.isIncompatible(resolved, fallbackEnabled)) {
            synchronized (lock) {
                state = tokenState;
                if (state == null || state.isExpired() || state.isIncompatible(resolved, fallbackEnabled)) {
                    tokenState = requestToken(resolved);
                    state = tokenState;
                }
            }
        }
        return state.accessToken();
    }

    /**
     * Logs in with provided credentials and caches the token.
     *
     * @param credentials login credentials
     */
    String login(MakerSpaceAuthCredentials credentials) {
        if (credentials == null || !StringUtils.hasText(credentials.username()) || !StringUtils.hasText(credentials.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required.");
        }
        synchronized (lock) {
            MakerSpaceAuthCredentials previousCredentials = runtimeCredentials;
            TokenState previousToken = tokenState;
            runtimeCredentials = credentials;
            try {
                tokenState = requestToken(new ResolvedCredentials(credentials, false));
            } catch (RuntimeException exception) {
                runtimeCredentials = previousCredentials;
                tokenState = previousToken;
                throw exception;
            }
        }
        return tokenState == null ? null : tokenState.accessToken();
    }

    public String login(AuthLoginRequest request) {
        if (request == null) {
            throw new AuthResponseException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        if (!StringUtils.hasText(request.username()) || !StringUtils.hasText(request.password())) {
            throw new AuthResponseException(HttpStatus.BAD_REQUEST, "Username and password are required.");
        }
        MakerSpaceAuthCredentials credentials = MakerSpaceAuthCredentials.from(request);
        try {
            String token = login(credentials);
            if (!StringUtils.hasText(token)) {
                throw new AuthResponseException(HttpStatus.UNAUTHORIZED, "Unable to retrieve access token.");
            }
            return token;
        } catch (RestClientResponseException exception) {
            String message = extractErrorDescription(exception.getResponseBodyAsString());
            HttpStatusCode status = exception.getStatusCode();
            HttpStatus resolved = status.value() == HttpStatus.BAD_REQUEST.value()
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.resolve(status.value());
            throw new AuthResponseException(resolved == null ? HttpStatus.UNAUTHORIZED : resolved, message);
        }
    }

    /**
     * Invalidates the current token, so the next call refreshes it.
     */
    public void invalidateToken() {
        if (resolveCredentials(isFallbackAllowed()) != null) {
            tokenState = null;
        }
    }

    /**
     * Forces the next token refresh to use fallback credentials.
     */
    public void forceFallbackNext() {
        useFallbackNext.set(true);
    }

    /**
     * Runs an action with fallback credentials enabled for the current thread.
     *
     * @param action action to execute
     */
    public void runWithFallback(Runnable action) {
        Boolean previous = fallbackAllowed.get();
        fallbackAllowed.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            fallbackAllowed.set(previous);
        }
    }

    /**
     * Logs out the current session and clears cached credentials.
     */
    public void logout() {
        String token = resolveLogoutToken();
        try {
            if (StringUtils.hasText(token)) {
                authClient.logout("Bearer " + token);
            }
            log.info("User was logged out.");
        } catch (InvalidMediaTypeException | RestClientResponseException ignored) {
        } finally {
            tokenState = null;
            runtimeCredentials = null;
        }
    }

    /**
     * Logs out when the application shuts down.
     */
    @PreDestroy
    void shutdown() {
        logout();
    }

    private ResolvedCredentials resolveCredentials(boolean fallbackEnabled) {
        if (fallbackEnabled && useFallbackNext.getAndSet(false)) {
            MakerSpaceAuthCredentials fallback = fallbackCredentials();
            if (fallback != null) {
                return new ResolvedCredentials(fallback, true);
            }
        }
        MakerSpaceAuthCredentials credentials = runtimeCredentials;
        if (credentials != null) {
            return new ResolvedCredentials(credentials, false);
        }
        if (fallbackEnabled) {
            MakerSpaceAuthCredentials fallback = fallbackCredentials();
            if (fallback != null) {
                return new ResolvedCredentials(fallback, true);
            }
        }
        return null;
    }

    private MakerSpaceAuthCredentials fallbackCredentials() {
        if (!StringUtils.hasText(properties.username()) || !StringUtils.hasText(properties.password())) {
            return null;
        }
        return MakerSpaceAuthCredentials.from(properties);
    }

    private String resolveLogoutToken() {
        TokenState current = tokenState;
        return current != null && StringUtils.hasText(current.accessToken()) ? current.accessToken() : null;
    }

    private TokenState requestToken(ResolvedCredentials resolved) {
        MakerSpaceAuthCredentials credentials = resolved.credentials();
        String username = credentials.username();
        String password = credentials.password();
        String clientId = StringUtils.hasText(credentials.clientId())
                ? credentials.clientId()
                : "nexudus.portal." + username;
        String totp = credentials.totp();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);
        form.add("totp", totp == null ? "" : totp);
        ResponseEntity<MakerSpaceTokenResponse> response = authClient.requestToken(form, clientId);
        MakerSpaceTokenResponse tokenResponse = response.getBody();
        String token = resolveToken(tokenResponse, response.getHeaders());
        if (!StringUtils.hasText(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to retrieve access token.");
        }
        Long expiresIn = tokenResponse != null ? tokenResponse.expiresIn() : null;
        Instant expiresAt = resolveExpiry(expiresIn);
        return new TokenState(token, expiresAt, credentials, resolved.fallback());
    }

    private String resolveToken(MakerSpaceTokenResponse tokenResponse, HttpHeaders headers) {
        String token = tokenResponse != null ? tokenResponse.accessToken() : null;
        if (StringUtils.hasText(token)) {
            return token;
        }
        return extractTokenFromHeaders(headers);
    }

    private Instant resolveExpiry(Long expiresIn) {
        Instant now = Instant.now();
        if (expiresIn == null || expiresIn <= 0) {
            return now.plus(DEFAULT_TOKEN_TTL).minus(REFRESH_BUFFER);
        }
        return now.plusSeconds(expiresIn).minus(REFRESH_BUFFER);
    }

    private String extractTokenFromHeaders(HttpHeaders headers) {
        String token = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(token)) {
            token = headers.getFirst("Bearer");
        }
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String normalized = token.trim();
        String lower = normalized.toLowerCase(Locale.ENGLISH);
        if (lower.startsWith("bearer ")) {
            return normalized.substring(7).trim();
        }
        return normalized;
    }

    private String extractErrorDescription(String body) {
        if (!StringUtils.hasText(body)) {
            return "Authentication failed.";
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode description = node.get("error_description");
            if (description != null && description.isTextual()) {
                return description.asText();
            }
            JsonNode message = node.get("message");
            if (message != null && message.isTextual()) {
                return message.asText();
            }
        } catch (Exception ignored) {
            return body;
        }
        return "Authentication failed.";
    }

    private record TokenState(
            String accessToken,
            Instant expiresAt,
            MakerSpaceAuthCredentials credentials,
            boolean fallback) {

        private boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }

        private boolean isIncompatible(ResolvedCredentials resolved, boolean fallbackEnabled) {
            if (fallback) {
                return !fallbackEnabled;
            }
            return resolved == null || credentials == null || !credentials.equals(resolved.credentials());
        }
    }

    private record ResolvedCredentials(MakerSpaceAuthCredentials credentials, boolean fallback) {
    }

    private boolean isFallbackAllowed() {
        return Boolean.TRUE.equals(fallbackAllowed.get());
    }
}
