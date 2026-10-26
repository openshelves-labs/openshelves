package com.openshelves.security.services;

import com.openshelves.common.model.SessionPrincipal;
import com.openshelves.security.entity.UserCredentialEntity;
import com.openshelves.security.entity.UserEntity;
import com.openshelves.security.entity.UserSessionEntity;
import com.openshelves.security.model.LoginRequest;
import com.openshelves.security.model.LoginResponse;
import com.openshelves.security.model.SessionResponse;
import com.openshelves.security.repository.UserCredentialRepository;
import com.openshelves.security.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/// Service for signing users in and out with session tokens.
///
/// Login verifies the credentials and opens a session; every later request is
/// authenticated by resolving its bearer token back to that session.
/// Tokens are opaque and random, and only their hash is stored.
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final Duration REMEMBER_ME_SESSION_TTL = Duration.ofDays(30);

    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final LockoutService lockoutService;

    private final UserCredentialRepository credentialRepository;
    private final UserSessionRepository sessionRepository;


    // -----------------------------------------------------------------------
    // Login & Logout
    // -----------------------------------------------------------------------

    /// Verifies the login credentials and opens a new session for the user.
    /// The session lasts 24 hours, or 30 days when a persistent ("remember me") login is requested.
    public LoginResponse createSession(LoginRequest request) {

        // Find the password credential for the email or institutional ID, ignoring stray whitespace
        String identifier = request.getIdentifier().strip();
        Optional<UserCredentialEntity> credential = credentialRepository.findByLoginIdentifier(identifier);

        // Reject locked accounts before the password is checked, so a lock can't be used
        // to test guesses, and attempts made while locked don't count
        credential.ifPresent(lockoutService::ensureCredentialNotLocked);

        // Verify the password before the account's status, so a wrong one never reveals it.
        // It runs even for an unknown identifier, so both cases take just as long.
        String passwordHash = credential.map(UserCredentialEntity::getPasswordHash).orElse(null);
        boolean passwordMatches = passwordService.verifyPassword(passwordHash, request.getPassword());

        if (credential.isEmpty() || !passwordMatches) {
            // Count the failure towards a lock; unknown identifiers have nothing to count against
            credential.ifPresent(c -> lockoutService.recordFailedAttempt(c.getId()));

            // Same message for an unknown identifier and a wrong password, so neither reveals which it was
            throw new BadCredentialsException("Invalid identifier or password. Repeated failures will temporarily lock your account.");
        }

        UserCredentialEntity userCredential = credential.get();

        // Reject disabled accounts (deleted ones were never matched)
        if (!userCredential.getUserProfile().isActive()) {
            throw new DisabledException("This account is disabled.");
        }

        // Clear any failures now that the login has succeeded
        lockoutService.recordSuccessfulLogin(userCredential.getId());

        // Open the session; only the token's hash is stored
        String token = tokenService.generateToken();

        UserSessionEntity session = buildSession(userCredential.getUserProfile(), token, request.isPersistent());
        sessionRepository.save(session);

        log.debug("Session opened [sessionId={}, persistent={}]", session.getId(), request.isPersistent());

        return LoginResponse.builder()
            .withToken(token)
            .withExpiresAt(session.getExpiresAt())
            .build();
    }

    /// Revokes the session with the given ID, signing that device out.
    /// Revoking a session that no longer exists does nothing.
    public void revokeSession(Long sessionId) {

        Optional<UserSessionEntity> session = sessionRepository.findById(sessionId);

        // Nothing to revoke
        if (session.isEmpty()) {
            return;
        }

        session.get().setRevokedAt(Instant.now());
        sessionRepository.save(session.get());

        log.debug("Session revoked [sessionId={}]", sessionId);
    }


    // -----------------------------------------------------------------------
    // Session Details
    // -----------------------------------------------------------------------

    /// Returns basic details of the user signed in with the given session.
    public SessionResponse getSessionDetails(Long sessionId) {

        UserSessionEntity session = sessionRepository.findWithUserProfileById(sessionId)
            .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Session not found."));

        return toSessionResponse(session.getUserProfile());
    }


    // -----------------------------------------------------------------------
    // Token Authentication
    // -----------------------------------------------------------------------

    /// Resolves a raw bearer token to an authenticated principal.
    ///
    /// @param sessionToken the raw token from the `Authorization` header
    /// @return the authentication, or empty if the token is unknown, expired or revoked
    public Optional<Authentication> authenticateSessionToken(String sessionToken) {

        // Look up the session by the token's hash, since the raw token is never stored
        String tokenHash = tokenService.hashToken(sessionToken);
        Optional<UserSessionEntity> session = sessionRepository.findBySessionTokenHash(tokenHash);

        // Unknown, revoked or expired sessions don't authenticate
        if (session.isEmpty() || !isSessionActive(session.get())) {
            return Optional.empty();
        }

        return Optional.of(buildAuthentication(session.get()));
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    /// Builds a new session for the user, expiring after the normal or remember-me lifetime.
    ///
    /// @param user the user signing in
    /// @param token the raw session token; only its hash is kept
    /// @param persistent whether a long-lived ("remember me") session was requested
    /// @return the session, ready to be saved
    private UserSessionEntity buildSession(UserEntity user, String token, boolean persistent) {
        Instant now = Instant.now();
        Duration ttl = persistent ? REMEMBER_ME_SESSION_TTL : SESSION_TTL;

        UserSessionEntity session = new UserSessionEntity();
        session.setUserProfile(user);
        session.setSessionTokenHash(tokenService.hashToken(token));
        session.setLastActiveAt(now);
        session.setExpiresAt(now.plus(ttl));

        return session;
    }

    /// Checks whether a session can still be used to authenticate.
    ///
    /// @param session the session, with its user loaded
    /// @return `true` if the session isn't revoked or expired, and its user is active and not deleted
    private boolean isSessionActive(UserSessionEntity session) {
        UserEntity user = session.getUserProfile();

        return session.getRevokedAt() == null
            && session.getExpiresAt().isAfter(Instant.now())
            && user.isActive()
            && user.getDeletedAt() == null;
    }

    /// Builds the authentication stored in the SecurityContext for a request.
    ///
    /// @param session the active session the request was authenticated with
    /// @return an authenticated token carrying the user and session IDs
    private Authentication buildAuthentication(UserSessionEntity session) {
        SessionPrincipal principal = SessionPrincipal.builder()
            .withUserId(session.getUserProfile().getId())
            .withSessionId(session.getId())
            .build();

        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }

    /// Maps the signed-in user to the session details response.
    ///
    /// @param user the user the session belongs to
    /// @return the basic user details for the frontend
    private SessionResponse toSessionResponse(UserEntity user) {
        return SessionResponse.builder()
            .withDisplayName(user.getDisplayName())
            .withEmail(user.getEmail())
            .withInstitutionalId(user.getInstitutionalId())
            .withDepartment(user.getDepartment())
            .withAvatarImageUrl(user.getAvatarImageUrl())
            .withLanguage(user.getLanguage())
            .withTimezone(user.getTimezone())
            .build();
    }
}
