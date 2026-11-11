package com.openshelves.security.exception;

/// The new password is the same as the current one.
///
/// Mapped to `400 PASSWORD_UNCHANGED`.
public class PasswordUnchangedException extends RuntimeException {

    public PasswordUnchangedException(String message) {
        super(message);
    }
}
