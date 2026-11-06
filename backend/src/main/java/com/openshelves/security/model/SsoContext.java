package com.openshelves.security.model;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;

/// What OpenShelves needs to remember across the identity-provider round-trip, set at `start`
/// and read at the callback. Travels in an encrypted cookie, since the API keeps no HTTP session.
@Value
@Builder(setterPrefix = "with")
public class SsoContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /// The provider being used.
    String providerKey;

    /// Whether a long-lived ("remember me") session was requested.
    boolean persistent;

    /// The frontend page to send the browser back to, already checked against the allowed origins.
    String frontendRedirectUrl;

    /// The user connecting the provider; `null` when signing in.
    Long linkUserId;

    /// PKCE challenge the exchange must answer; `null` when connecting a provider.
    String codeChallenge;

    /// Whether this round-trip connects a provider rather than signing in.
    public boolean isLink() {
        return linkUserId != null;
    }
}
