package com.openshelves.security.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/// Response body for `GET /api/v1/auth/session`.
///
/// The small set of data every page needs to render the app shell. Called on every app load,
/// so it stays small; the full profile belongs to the profile endpoints.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class SessionResponse {

    /// Only what the app shell displays or formats with.
    User user;

    /// When the session expires, in UTC.
    Instant expiresAt;

    /// Whether this is a long-lived ("keep me signed in") session.
    boolean persistent;


    /// The signed-in user, as shown in the app shell.
    @Value
    @Builder(setterPrefix = "with")
    @Jacksonized
    public static class User {

        String displayName;

        String avatarImageUrl;

        /// ISO language code.
        String language;

        /// IANA time zone, e.g. `Asia/Kolkata`.
        String timezone;
    }
}
