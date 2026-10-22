package com.openshelves.security.services;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    /// Resolves a raw bearer token to an authenticated principal.
    ///
    /// @param token the raw token from the `Authorization` header
    /// @return the authentication, or empty if the token is unknown, expired or revoked
    public Optional<Authentication> authenticate(String token) {
        // TODO: Hash the token, look up the session, and check expiry, revocation and user status
        return Optional.empty();
    }
}
