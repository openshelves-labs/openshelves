package com.openshelves.security.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/// Response body for `GET /api/v1/auth/user-session`.
///
/// Basic details of the signed-in user, safe for the frontend to cache until [#expiresAt] or logout.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class SessionResponse {

    Long userId;

    String displayName;

    String email;

    /// Library card or student ID.
    String institutionalId;

    String department;

    String program;

    String avatarImageUrl;

    /// ISO language code.
    String language;

    /// IANA time zone, e.g. `Asia/Kolkata`.
    String timezone;

    /// When the session expires, in UTC.
    Instant expiresAt;
}
