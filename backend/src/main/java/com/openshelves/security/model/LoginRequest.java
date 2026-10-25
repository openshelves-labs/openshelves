package com.openshelves.security.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/// Request body for `POST /api/v1/auth/login`.
///
/// Used for local id/password login; identity-provider logins don't use this body.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class LoginRequest {

    /// Email or institutional ID.
    @NotBlank
    String identifier;

    /// Raw password; never logged.
    @NotNull
    @ToString.Exclude
    String password;

    /// Opens a long-lived ("remember me") session instead of a normal one.
    boolean persistent;
}
