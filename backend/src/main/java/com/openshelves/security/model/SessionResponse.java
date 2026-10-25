package com.openshelves.security.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/// Response body for `GET /api/v1/auth/session`.
///
/// Basic details of the signed-in user.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class SessionResponse {

    String displayName;

    String email;

    /// Library card or student ID.
    String institutionalId;

    String department;

    String avatarImageUrl;

    /// ISO language code.
    String language;

    /// IANA time zone, e.g. `Asia/Kolkata`.
    String timezone;
}
