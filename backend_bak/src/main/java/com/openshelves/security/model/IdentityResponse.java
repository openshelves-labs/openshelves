package com.openshelves.security.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/// One entry of `GET /api/v1/auth/identities`: an identity provider the user has connected.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class IdentityResponse {

    Long id;

    /// Key of the matching provider; absent if that provider no longer exists.
    String providerKey;

    /// Display name of the matching provider; absent if that provider no longer exists.
    String providerName;

    /// Email or name the provider sent, e.g. "arpan@uni.edu"; absent if it sent none.
    String linkedAs;

    Instant createdAt;

    Instant lastUsedAt;
}
