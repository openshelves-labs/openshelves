package com.openshelves.security.model;

import com.openshelves.security.enums.AuthProviderType;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/// One entry of `GET /api/v1/auth/providers`: an option in the sign-in page's SSO dropdown.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class ProviderResponse {

    /// Label used in the SSO URLs, e.g. `campus-shibboleth`.
    String key;

    AuthProviderType type;

    String displayName;
}
