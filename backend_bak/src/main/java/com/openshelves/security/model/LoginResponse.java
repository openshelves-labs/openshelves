package com.openshelves.security.model;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/// Response body for `POST /api/v1/auth/login`.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class LoginResponse {

    /// Opaque bearer token.
    @ToString.Exclude
    String token;

    /// When the session expires, in UTC.
    Instant expiresAt;
}
