package com.openshelves.security.services;

import com.openshelves.common.model.SessionPrincipal;
import com.openshelves.security.entity.UserCredentialEntity;
import com.openshelves.security.entity.UserEntity;
import com.openshelves.security.entity.UserSessionEntity;
import com.openshelves.security.enums.CredentialType;
import com.openshelves.security.model.LoginRequest;
import com.openshelves.security.model.LoginResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/// Service for creating, resolving and revoking login sessions.
///
/// Sessions use opaque random tokens; only their SHA-256 hash is stored.
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private static final int SESSION_TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    private final PasswordEncoder passwordEncoder;


    // -------------------------------------------------------------------------
    // MOCK: In-memory user and sessions, until repositories are in place
    // -------------------------------------------------------------------------

    /// Sign in with `admin@openshelves.local` or `ADMIN001`, password `admin`.
    private UserCredentialEntity mockCredential;

    /// Sessions keyed by token hash, standing in for the `user_sessions` table.
    private final Map<String, UserSessionEntity> mockSessions = new ConcurrentHashMap<>();

    private final AtomicLong mockSessionIds = new AtomicLong();

    /// Runs after injection, since the mock password is hashed with the injected encoder.
    @PostConstruct
    private void initMockCredential() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setDisplayName("Admin");
        user.setEmail("admin@openshelves.local");
        user.setInstitutionalId("ADMIN001");

        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setId(1L);
        credential.setUserProfile(user);
        credential.setCredentialType(CredentialType.PASSWORD);
        credential.setPasswordHash(passwordEncoder.encode("admin"));

        mockCredential = credential;
    }


    // -------------------------------------------------------------------------
    // Session lifecycle
    // -------------------------------------------------------------------------

    /// Verifies the credentials and starts a new session.
    public LoginResponse createSession(LoginRequest request) {
        // TODO: Check lockout and track failed attempts
        UserCredentialEntity credential = findPasswordCredential(request.getIdentifier())
            .filter(c -> verifyPassword(c, request.getPassword()))
            .orElseThrow(() -> new BadCredentialsException("Invalid identifier or password."));

        String token = generateSessionToken();
        Instant now = Instant.now();

        UserSessionEntity session = new UserSessionEntity();
        session.setId(mockSessionIds.incrementAndGet());
        session.setUserProfile(credential.getUserProfile());
        session.setSessionTokenHash(hashSessionToken(token));
        session.setLastActiveAt(now);
        session.setExpiresAt(now.plus(SESSION_TTL));
        session.setCreatedAt(now);

        // TODO: Save through the session repository
        mockSessions.put(session.getSessionTokenHash(), session);

        return LoginResponse.builder()
            .withToken(token)
            .withExpiresAt(session.getExpiresAt())
            .build();
    }

    /// Revokes the session with the given ID.
    public void revokeSession(Long sessionId) {
        // TODO: Find the session through the repository
        mockSessions.values().stream()
            .filter(s -> s.getId().equals(sessionId))
            .findFirst()
            .ifPresent(s -> s.setRevokedAt(Instant.now()));
    }


    // -------------------------------------------------------------------------
    // Token authentication
    // -------------------------------------------------------------------------

    /// Resolves a raw bearer token to an authenticated principal.
    ///
    /// @param sessionToken the raw token from the `Authorization` header
    /// @return the authentication, or empty if the token is unknown, expired or revoked
    public Optional<Authentication> authenticateSessionToken(String sessionToken) {
        // TODO: Look up the session through the repository
        UserSessionEntity session = mockSessions.get(hashSessionToken(sessionToken));

        if (session == null || !isSessionActive(session)) {
            return Optional.empty();
        }

        SessionPrincipal principal = SessionPrincipal.builder()
            .withUserId(session.getUserProfile().getId())
            .withSessionId(session.getId())
            .build();

        return Optional.of(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }

    /// Checks the session is not revoked or expired, and its user is active.
    private boolean isSessionActive(UserSessionEntity session) {
        return session.getRevokedAt() == null
            && session.getExpiresAt().isAfter(Instant.now())
            && session.getUserProfile().isActive();
    }


    // -------------------------------------------------------------------------
    // Credential checks
    // -------------------------------------------------------------------------

    /// Finds the password credential for a username, email, or library card ID.
    private Optional<UserCredentialEntity> findPasswordCredential(String identifier) {
        // TODO: Look up through the credential repository
        UserEntity user = mockCredential.getUserProfile();

        boolean matches = user.getEmail().equalsIgnoreCase(identifier)
            || user.getInstitutionalId().equals(identifier);

        return matches ? Optional.of(mockCredential) : Optional.empty();
    }

    /// Checks the raw password against the stored hash.
    private boolean verifyPassword(UserCredentialEntity credential, String rawPassword) {
        return rawPassword != null && passwordEncoder.matches(rawPassword, credential.getPasswordHash());
    }


    // -------------------------------------------------------------------------
    // Session tokens
    // -------------------------------------------------------------------------

    /// Generates a new random, opaque session token.
    private String generateSessionToken() {
        byte[] bytes = new byte[SESSION_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /// Hashes a raw session token (SHA-256) for storage and lookup.
    private String hashSessionToken(String sessionToken) {
        return DigestUtils.sha256Hex(sessionToken);
    }
}
