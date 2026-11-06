package com.openshelves.security.services;

import com.openshelves.common.email.EmailSender;
import com.openshelves.common.model.SessionPrincipal;
import com.openshelves.security.entity.PasswordResetTokenEntity;
import com.openshelves.security.entity.UserEntity;
import com.openshelves.security.entity.UserPasswordEntity;
import com.openshelves.security.exception.InvalidCurrentPasswordException;
import com.openshelves.security.exception.PasswordUnchangedException;
import com.openshelves.security.exception.ResetLinkExpiredException;
import com.openshelves.security.repository.PasswordResetTokenRepository;
import com.openshelves.security.repository.UserPasswordRepository;
import com.openshelves.security.repository.UserRepository;
import com.openshelves.security.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/// Service for a user's password: changing it while signed in, and setting it through an emailed link.
///
/// The emailed link covers both forgot password and first-time setup, since there's no sign-up:
/// it creates the password if the user doesn't have one yet (e.g. a provisioned or SSO-only user).
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPasswordService {

    /// How long an emailed link works.
    private static final Duration RESET_LINK_TTL = Duration.ofMinutes(30);

    /// Requests for the same user closer together than this are ignored, so an inbox can't be flooded.
    private static final Duration RESET_REQUEST_INTERVAL = Duration.ofMinutes(1);

    private final PasswordService passwordService;
    private final LockoutService lockoutService;
    private final TokenService tokenService;
    private final EmailSender emailSender;

    private final UserRepository userRepository;
    private final UserPasswordRepository passwordRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordResetTokenRepository resetTokenRepository;


    // -----------------------------------------------------------------------
    // Change
    // -----------------------------------------------------------------------

    /// Changes the signed-in user's password, and signs out every other device.
    ///
    /// Not one transaction on purpose: a wrong current password must still be counted towards
    /// the lockout, which [LockoutService] saves in its own transaction.
    ///
    /// @param principal the caller
    /// @param currentPassword the password the user signs in with today
    /// @param newPassword the new password, already validated for length
    /// @throws InvalidCurrentPasswordException if the current password is wrong, or the user has no password
    /// @throws PasswordUnchangedException if the new password is the same as the current one
    /// @throws org.springframework.security.authentication.LockedException if too many wrong attempts locked the password
    public void changePassword(SessionPrincipal principal, String currentPassword, String newPassword) {
        UserPasswordEntity userPassword = passwordRepository.findByUserProfileId(principal.getUserId())
            .orElseThrow(() -> new InvalidCurrentPasswordException("The current password is wrong."));

        // Same protection as login: locked passwords aren't checked, wrong ones count towards a lock
        lockoutService.ensurePasswordNotLocked(userPassword);
        if (!passwordService.verifyPassword(userPassword.getPasswordHash(), currentPassword)) {
            lockoutService.recordFailedAttempt(userPassword.getId());
            throw new InvalidCurrentPasswordException("The current password is wrong.");
        }

        if (passwordService.verifyPassword(userPassword.getPasswordHash(), newPassword)) {
            throw new PasswordUnchangedException("The new password must be different from the current one.");
        }

        // Clears any failed attempts, then stores the new password on the freshly saved row
        lockoutService.recordSuccessfulLogin(userPassword.getId());
        UserPasswordEntity updated = passwordRepository.findById(userPassword.getId()).orElseThrow();
        updated.setPasswordHash(passwordService.encodePassword(newPassword));
        updated.setPasswordChangedAt(Instant.now());
        passwordRepository.save(updated);

        int revoked = sessionRepository.revokeAllExcept(principal.getUserId(), principal.getSessionId(), Instant.now());
        log.debug("Password changed [userId={}, otherSessionsRevoked={}]", principal.getUserId(), revoked);
    }


    // -----------------------------------------------------------------------
    // Reset by Email
    // -----------------------------------------------------------------------

    /// Emails the user a link to set a new password, if there's an active user with an email.
    ///
    /// Says nothing about the outcome, so callers can't use it to find accounts. A new link replaces
    /// any earlier one; requests within [#RESET_REQUEST_INTERVAL] of the last are ignored.
    ///
    /// @param identifier email or institutional ID
    /// @param frontendResetUrl the frontend page the link opens, already checked against CORS
    @Transactional
    public void requestResetLink(String identifier, String frontendResetUrl) {
        Optional<UserEntity> user = userRepository.findByLoginIdentifier(identifier.strip())
            .filter(UserEntity::isActive)
            .filter(u -> u.getEmail() != null && !u.getEmail().isBlank());

        if (user.isEmpty()) {
            log.debug("Password reset link not sent: no active user with an email");
            return;
        }

        Long userId = user.get().getId();
        Instant now = Instant.now();
        if (resetTokenRepository.existsByUserProfileIdAndCreatedAtAfter(userId, now.minus(RESET_REQUEST_INTERVAL))) {
            log.debug("Password reset link not sent: one was just sent [userId={}]", userId);
            return;
        }

        // Only the newest link works
        resetTokenRepository.deleteAllByUserId(userId);

        String token = tokenService.generateToken();
        PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();
        resetToken.setUserProfile(user.get());
        resetToken.setTokenHash(tokenService.hashToken(token));
        resetToken.setExpiresAt(now.plus(RESET_LINK_TTL));
        resetTokenRepository.save(resetToken);

        String link = UriComponentsBuilder.fromUriString(frontendResetUrl)
            .queryParam("token", UriUtils.encodeQueryParam(token, StandardCharsets.UTF_8))
            .build(true)    // frontendResetUrl is already encoded; the token is encoded above
            .toUriString();

        emailSender.send(user.get().getEmail(), "Set your OpenShelves password", """
            Hello %s,

            Use this link to set a new password for OpenShelves. It works once, for the next %d minutes:

            %s

            If you didn't ask for this, you can ignore this email; your password stays as it is.
            """.formatted(user.get().getDisplayName(), RESET_LINK_TTL.toMinutes(), link));

        log.debug("Password reset link sent [userId={}]", userId);
    }

    /// Sets a new password with the token from an emailed link, creating the password if the user has none.
    /// Clears any lockout and signs out every device; the user then signs in normally.
    ///
    /// The token is used up by any attempt, even a failed one, so a stolen link can't be retried.
    ///
    /// @param token the raw token from the link
    /// @param newPassword the new password, already validated for length
    /// @throws ResetLinkExpiredException if the token is unknown, used or expired
    /// @throws DisabledException if the user was disabled since the link was sent
    @Transactional(noRollbackFor = {ResetLinkExpiredException.class, AuthenticationException.class})    // Keep the token deleted when refused
    public void resetPassword(String token, String newPassword) {
        PasswordResetTokenEntity resetToken = resetTokenRepository.findAndLockByTokenHash(tokenService.hashToken(token))
            .orElseThrow(() -> new ResetLinkExpiredException("This link is invalid or has expired. Please ask for a new one."));
        resetTokenRepository.delete(resetToken);

        Instant now = Instant.now();
        if (resetToken.getExpiresAt().isBefore(now)) {
            throw new ResetLinkExpiredException("This link is invalid or has expired. Please ask for a new one.");
        }

        UserEntity user = resetToken.getUserProfile();
        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new DisabledException("This account is disabled.");
        }

        // Update the password, or create it for a user who never had one
        UserPasswordEntity userPassword = passwordRepository.findByUserProfileId(user.getId()).orElseGet(() -> {
            UserPasswordEntity created = new UserPasswordEntity();
            created.setUserProfile(user);
            return created;
        });
        userPassword.setPasswordHash(passwordService.encodePassword(newPassword));
        userPassword.setPasswordChangedAt(now);
        userPassword.setFailedAttempts(0);
        userPassword.setLockedUntil(null);
        passwordRepository.save(userPassword);

        int revoked = sessionRepository.revokeAll(user.getId(), now);
        log.debug("Password reset [userId={}, sessionsRevoked={}]", user.getId(), revoked);
    }
}
