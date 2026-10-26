package com.openshelves.security.services;

import com.openshelves.security.entity.UserCredentialEntity;
import com.openshelves.security.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/// Service for tracking failed logins and locking credentials after repeated failures.
///
/// Every 5 consecutive failures lock the credential, for longer each time:
/// 15 minutes, then 30 minutes, then 24 hours for every lock after that.
/// Locks always expire on their own; a successful login clears the count.
@Slf4j
@Service
@RequiredArgsConstructor
public class LockoutService {

    private static final int FAILURES_PER_LOCK = 5;

    /// Lock duration for the 1st, 2nd and 3rd lock; the last one repeats for every lock after.
    private static final List<Duration> LOCK_DURATIONS = List.of(
        Duration.ofMinutes(15),
        Duration.ofMinutes(30),
        Duration.ofHours(24)
    );

    private final UserCredentialRepository credentialRepository;


    // -----------------------------------------------------------------------
    // Lock Check
    // -----------------------------------------------------------------------

    /// Rejects a login to a credential that is currently locked.
    ///
    /// Called before the password is checked, so a locked account answers the same way
    /// whatever password is sent, and attempts made while locked don't count.
    ///
    /// @param credential the credential the user is signing in with
    /// @throws LockedException if the credential is locked
    public void ensureCredentialNotLocked(UserCredentialEntity credential) {
        if (isLocked(credential)) {
            // The message is shown to the user, so it includes the time left until the lock expires
            String message = buildLockedMessage(credential.getLockedUntil());
            throw new LockedException(message);
        }
    }


    // -----------------------------------------------------------------------
    // Attempt Tracking
    // -----------------------------------------------------------------------

    /// Counts a failed login, and locks the credential when the count reaches the next threshold.
    ///
    /// Runs in its own transaction, so the count is saved even though the login itself fails.
    ///
    /// @param credentialId the credential whose password was wrong
    @Transactional
    public void recordFailedAttempt(Long credentialId) {

        // Lock the row, so parallel failures can't overwrite each other's count
        UserCredentialEntity credential = credentialRepository.findAndLockById(credentialId).orElseThrow();

        // Locked by a parallel request since this one started; attempts made while locked don't count
        if (isLocked(credential)) {
            return;
        }

        int failedAttempts = credential.getFailedAttempts() + 1;
        credential.setFailedAttempts(failedAttempts);

        // Every FAILURES_PER_LOCK-th failure locks the credential
        if (failedAttempts % FAILURES_PER_LOCK == 0) {
            Duration lockDuration = lockDurationFor(failedAttempts);
            credential.setLockedUntil(Instant.now().plus(lockDuration));

            log.warn("Credential locked [credentialId={}, failedAttempts={}, duration={}]", credentialId, failedAttempts, lockDuration);
        }

        credentialRepository.save(credential);
    }

    /// Clears the failure count and any lock after a successful login, and records when it was used.
    ///
    /// @param credentialId the credential the user signed in with
    /// @throws LockedException if a parallel request locked the credential since this login started
    @Transactional
    public void recordSuccessfulLogin(Long credentialId) {
        UserCredentialEntity credential = credentialRepository.findAndLockById(credentialId).orElseThrow();

        // Locked by a parallel request since this one started; a correct password mustn't lift the lock
        ensureCredentialNotLocked(credential);

        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credential.setLastUsedAt(Instant.now());

        credentialRepository.save(credential);
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    /// Returns how long to lock the credential for, based on how many locks it has had in a row.
    ///
    /// @param failedAttempts the failure count that triggered this lock (a multiple of [#FAILURES_PER_LOCK])
    /// @return 15 minutes for the 1st lock, 30 for the 2nd, and 24 hours from the 3rd on
    private Duration lockDurationFor(int failedAttempts) {
        int lockNumber = failedAttempts / FAILURES_PER_LOCK;
        int index = Math.min(lockNumber, LOCK_DURATIONS.size()) - 1;

        return LOCK_DURATIONS.get(index);
    }

    /// Checks whether the credential is locked right now.
    ///
    /// @param credential the credential to check
    /// @return `true` if it has a lock that hasn't expired yet
    private boolean isLocked(UserCredentialEntity credential) {
        Instant lockedUntil = credential.getLockedUntil();
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    /// Builds the message shown for a locked credential, with the time left until it unlocks.
    ///
    /// The time is rounded up, in minutes below an hour and in hours above,
    /// e.g. "Try again in 12 minutes." or "Try again in 24 hours."
    ///
    /// @param lockedUntil when the lock expires
    /// @return the message for the error response
    private String buildLockedMessage(Instant lockedUntil) {
        long secondsLeft = Duration.between(Instant.now(), lockedUntil).toSeconds();
        long minutesLeft = Math.max(1, Math.ceilDiv(secondsLeft, 60));

        String timeLeft = minutesLeft < 60
            ? pluralize(minutesLeft, "minute")
            : pluralize(Math.ceilDiv(minutesLeft, 60), "hour");

        return "This account is temporarily locked. Try again in " + timeLeft + ".";
    }

    /// Formats a count with its unit, e.g. "1 minute" or "12 minutes".
    private String pluralize(long count, String unit) {
        return count + " " + (count == 1 ? unit : unit + "s");
    }
}
