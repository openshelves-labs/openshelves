package com.openshelves.security.exception;

/// The current password given when changing passwords is wrong, or the user has no password.
///
/// Mapped to `400 INVALID_CURRENT_PASSWORD`.
public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException(String message) {
        super(message);
    }
}
