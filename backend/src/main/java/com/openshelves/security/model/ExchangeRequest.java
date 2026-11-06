package com.openshelves.security.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/// Request body for `POST /api/v1/auth/sso/exchange`.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class ExchangeRequest {

    /// One-time code from the SSO callback redirect; never logged.
    @NotBlank
    @ToString.Exclude
    String code;
}
