package com.openshelves.security.model;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/// Request body for `POST /api/v1/auth/login`.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class LoginRequest {

    /// Username, email, or library card ID.
    String identifier;

    @ToString.Exclude
    String password;
}
