package com.openshelves.security.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/// Service for checking passwords against their stored hashes.
///
/// Hashing itself is delegated to the configured [PasswordEncoder].
/// Checks take the same time whether or not the account exists, so failed
/// logins can't be used to discover which identifiers are registered.
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    /// Stand-in password checked when the user isn't found; its value doesn't matter, since the result is discarded.
    private static final String USER_NOT_FOUND_PASSWORD = "userNotFoundPassword";

    /// Encoded [#USER_NOT_FOUND_PASSWORD], checked in place of a real hash when the user isn't found.
    private String userNotFoundEncodedPassword;

    /// Encodes the stand-in password once at startup, so it's ready for [#mitigateAgainstTimingAttack].
    @PostConstruct
    private void prepareTimingAttackProtection() {
        userNotFoundEncodedPassword = passwordEncoder.encode(USER_NOT_FOUND_PASSWORD);
    }


    // -----------------------------------------------------------------------
    // Password Verification
    // -----------------------------------------------------------------------

    /// Checks a raw password against its stored hash.
    ///
    /// When there's no stored hash (an unknown account), the check still takes as long
    /// as a wrong password, so it doesn't reveal that the account is missing.
    ///
    /// @param passwordHash the stored hash, or `null` if the account wasn't found
    /// @param rawPassword the password as entered by the user
    /// @return `true` only if the password matches the stored hash; `false` if the stored hash is malformed
    public boolean verifyPassword(@Nullable String passwordHash, String rawPassword) {

        // Nothing to check
        if (rawPassword == null) {
            return false;
        }

        // Unknown account: spend the same time as a real check, then fail
        if (passwordHash == null) {
            mitigateAgainstTimingAttack(rawPassword);
            return false;
        }

        // A hash the encoder can't read (e.g. missing its `{bcrypt}` prefix) fails the login instead of erroring
        try {
            return passwordEncoder.matches(rawPassword, passwordHash);
        } catch (IllegalArgumentException e) {
            log.error("Stored password hash is malformed; it must carry an encoder prefix such as {bcrypt}", e);
            return false;
        }
    }

    /// Runs a password check against the stand-in hash and discards the result,
    /// so a missing account takes as long to reject as a wrong password.
    ///
    /// @param rawPassword the password as entered by the user
    private void mitigateAgainstTimingAttack(String rawPassword) {
        passwordEncoder.matches(rawPassword, userNotFoundEncodedPassword);
    }
}
