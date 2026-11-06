package com.openshelves.security.sso;

import java.io.Serial;
import java.io.Serializable;

/// What OpenShelves needs to remember across the identity-provider round-trip, set at `start`
/// and read at the callback. Travels in an encrypted cookie, since the API keeps no HTTP session.
///
/// @param providerKey the provider being used
/// @param persistent  whether a long-lived ("remember me") session was requested
/// @param returnTo    frontend path to open after sign-in; `null` for none
/// @param linkUserId  the user connecting the provider; `null` when signing in
public record SsoContext(String providerKey, boolean persistent, String returnTo, Long linkUserId) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /// Whether this round-trip connects a provider rather than signing in.
    public boolean isLink() {
        return linkUserId != null;
    }
}
