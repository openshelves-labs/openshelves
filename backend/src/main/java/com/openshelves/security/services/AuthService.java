package com.openshelves.security.services;

import com.openshelves.common.model.SessionPrincipal;
import com.openshelves.security.entity.UserCredentialEntity;
import com.openshelves.security.model.LoginRequest;
import com.openshelves.security.model.LoginResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    // -------------------------------------------------------------------------
    // Session lifecycle
    // -------------------------------------------------------------------------

    /// Verifies the credentials and starts a new session.
    public LoginResponse createSession(LoginRequest request) {
        // TODO: Find the credential, check lockout and password, then create a session
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /// Revokes the session with the given ID.
    public void revokeSession(Long sessionId) {
        // TODO: Find the session and set revokedAt
    }


    // -------------------------------------------------------------------------
    // Token authentication
    // -------------------------------------------------------------------------

    /// Resolves a raw bearer token to an authenticated principal.
    ///
    /// @param sessionToken the raw token from the `Authorization` header
    /// @return the authentication, or empty if the token is unknown, expired or revoked
    public Optional<Authentication> authenticateSessionToken(String sessionToken) {
        // TODO: Hash the token, look up the session, and check expiry, revocation and user status

        // MOCK: Accepts any token as user 1, session 1, until session lookup is implemented
        SessionPrincipal principal = SessionPrincipal.builder()
            .withUserId(1L)
            .withSessionId(1L)
            .build();

        return Optional.of(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }


    // -------------------------------------------------------------------------
    // Credential checks
    // -------------------------------------------------------------------------

    /// Finds the password credential for a username, email, or library card ID.
    private Optional<UserCredentialEntity> findPasswordCredential(String identifier) {
        // TODO: implement
        return Optional.empty();
    }

    /// Checks the raw password against the stored hash.
    private boolean verifyPassword(UserCredentialEntity credential, String rawPassword) {
        // TODO: implement
        return false;
    }


    // -------------------------------------------------------------------------
    // Session tokens
    // -------------------------------------------------------------------------

    /// Generates a new random, opaque session token.
    private String generateSessionToken() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /// Hashes a raw session token (SHA-256) for storage and lookup.
    private String hashSessionToken(String sessionToken) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
