package com.openshelves.security.services;

import com.openshelves.common.model.SessionPrincipal;
import com.openshelves.security.entity.UserEntity;
import com.openshelves.security.entity.UserPasswordEntity;
import com.openshelves.security.entity.UserSessionEntity;
import com.openshelves.security.model.DeviceInfo;
import com.openshelves.security.model.LoginRequest;
import com.openshelves.security.model.LoginResponse;
import com.openshelves.security.model.SessionResponse;
import com.openshelves.security.repository.UserPasswordRepository;
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

    /// A session's `last_active_at` is written at most once per this window.
    private static final Duration ACTIVITY_WRITE_INTERVAL = Duration.ofMinutes(1);

    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final LockoutService lockoutService;

    private final UserPasswordRepository passwordRepository;
    private final UserSessionRepository sessionRepository;


    // -----------------------------------------------------------------------
    // Login & Logout
    // -----------------------------------------------------------------------

    /// Verifies the login credentials and opens a new session for the user.
    /// The session lasts 24 hours, or 30 days when a persistent ("remember me") login is requested.
    ///
    /// @param request the login credentials
    /// @param device  the device the login comes from, recorded on the session
    public LoginResponse createSession(LoginRequest request, DeviceInfo device) {

        // Find the password for the email or institutional ID, ignoring stray whitespace
        String identifier = request.getIdentifier().strip();
        Optional<UserPasswordEntity> userPassword = passwordRepository.findByLoginIdentifier(identifier);

        // Reject locked accounts before the password is checked, so a lock can't be used
        // to test guesses, and attempts made while locked don't count
        userPassword.ifPresent(lockoutService::ensurePasswordNotLocked);

        // Verify the password before the account's status, so a wrong one never reveals it.
        // It runs even for an unknown identifier, so both cases take just as long.
        String passwordHash = userPassword.map(UserPasswordEntity::getPasswordHash).orElse(null);
        boolean passwordMatches = passwordService.verifyPassword(passwordHash, request.getPassword());

        if (userPassword.isEmpty() || !passwordMatches) {
            // Count the failure towards a lock; unknown identifiers have nothing to count against
            userPassword.ifPresent(p -> lockoutService.recordFailedAttempt(p.getId()));

            // Same message for an unknown identifier and a wrong password, so neither reveals which it was
            throw new BadCredentialsException("Invalid identifier or password. Repeated failures will temporarily lock your account.");
        }

        UserEntity user = userPassword.get().getUserProfile();

        // Reject disabled accounts (deleted ones were never matched)
        if (!user.isActive()) {
            throw new DisabledException("This account is disabled.");
        }

        // Clear any failures now that the login has succeeded
        lockoutService.recordSuccessfulLogin(userPassword.get().getId());

        // Open the session; only the token's hash is stored.
        // No auth provider: this session was opened with a password.
        String token = tokenService.generateToken();

        UserSessionEntity session = buildSession(user, token, request.isPersistent(), device, null);
        sessionRepository.save(session);

        log.debug("Session opened [sessionId={}, persistent={}]", session.getId(), request.isPersistent());

        return LoginResponse.builder()
            .withToken(token)
            .withExpiresAt(session.getExpiresAt())
            .build();
    }

    /// Revokes the session with the given ID, signing that device out.
    /// Revoking a session that no longer exists, or is already revoked, does nothing.
    public void revokeSession(Long sessionId) {

        Optional<UserSessionEntity> session = sessionRepository.findById(sessionId);

        // Nothing to revoke
        if (session.isEmpty() || session.get().getRevokedAt() != null) {
            return;
        }

        session.get().setRevokedAt(Instant.now());
        sessionRepository.save(session.get());

        log.debug("Session revoked [sessionId={}]", sessionId);
    }


    // -----------------------------------------------------------------------
    // Session Details
    // -----------------------------------------------------------------------

    /// Returns the current session and the app-shell details of its user.
    public SessionResponse getSessionDetails(Long sessionId) {

        UserSessionEntity session = sessionRepository.findWithUserProfileById(sessionId)
            .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Session not found."));

        return toSessionResponse(session);
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

        recordActivity(session.get());

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
    /// @param device the device the login comes from
    /// @param authProvider display name of the identity provider used, or `null` for a password
    /// @return the session, ready to be saved
    private UserSessionEntity buildSession(UserEntity user, String token, boolean persistent, DeviceInfo device, String authProvider) {
        Instant now = Instant.now();
        Duration ttl = persistent ? REMEMBER_ME_SESSION_TTL : SESSION_TTL;

        UserSessionEntity session = new UserSessionEntity();
        session.setUserProfile(user);
        session.setSessionTokenHash(tokenService.hashToken(token));

        // Device
        session.setIpAddress(device.ipAddress());
        session.setUserAgent(device.userAgent());
        session.setDeviceName(device.deviceName());
        session.setLastActiveAt(now);

        // Sign-in method and lifetime
        session.setAuthProvider(authProvider);
        session.setPersistent(persistent);
        session.setExpiresAt(now.plus(ttl));

        return session;
    }

    /// Records that the session was just used, for the devices list.
    ///
    /// Written at most once per [#ACTIVITY_WRITE_INTERVAL], so busy sessions don't cause a write per request.
    /// The in-memory check skips the query when it can't update anything; the conditional update
    /// keeps parallel requests from writing more than once.
    ///
    /// @param session the active session the request was authenticated with
    private void recordActivity(UserSessionEntity session) {
        Instant now = Instant.now();
        Instant threshold = now.minus(ACTIVITY_WRITE_INTERVAL);

        if (session.getLastActiveAt().isBefore(threshold)) {
            sessionRepository.touchLastActiveAt(session.getId(), now, threshold);
        }
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

    /// Maps the current session and its user to the session details response.
    ///
    /// @param session the current session, with its user loaded
    /// @return the session details for the app shell
    private SessionResponse toSessionResponse(UserSessionEntity session) {
        UserEntity user = session.getUserProfile();

        SessionResponse.User shellUser = SessionResponse.User.builder()
            .withDisplayName(user.getDisplayName())
            .withAvatarImageUrl(user.getAvatarImageUrl())
            .withLanguage(user.getLanguage())
            .withTimezone(user.getTimezone())
            .build();

        return SessionResponse.builder()
            .withUser(shellUser)
            .withExpiresAt(session.getExpiresAt())
            .withPersistent(session.isPersistent())
            .build();
    }
}
