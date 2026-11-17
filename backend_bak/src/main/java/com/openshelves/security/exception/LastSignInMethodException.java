package com.openshelves.security.exception;

/// The user tried to disconnect their only way to sign in (no password, no other identity).
///
/// Mapped to `409 LAST_SIGN_IN_METHOD`.
public class LastSignInMethodException extends RuntimeException {

    public LastSignInMethodException(String message) {
        super(message);
    }
}
