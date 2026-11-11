package com.openshelves.security.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/// Response body for `POST /api/v1/auth/identities/{provider}`.
@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class LinkResponse {

    /// Where the browser goes next: the SSO start URL, carrying a one-time link code.
    String redirectUrl;
}
