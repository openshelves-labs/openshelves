package com.openshelves.security.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    /// PKCE verifier: the random secret whose S256 hash was sent as `codeChallenge` at start (RFC 7636); never logged.
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9._~-]{43,128}")
    @ToString.Exclude
    String codeVerifier;
}
