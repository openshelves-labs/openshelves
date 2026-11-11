package com.openshelves.security.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/// Request body for `POST /api/v1/auth/password/reset-request`.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class ResetLinkRequest {

    /// Email or institutional ID.
    @NotBlank
    String identifier;

    /// The frontend page the emailed link opens, e.g. `https://app.uni.edu/reset-password`;
    /// its origin must be allowed by CORS.
    @NotBlank
    String frontendResetUrl;
}
