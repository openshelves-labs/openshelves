package com.openshelves.security.exception;

/// The password reset link is unknown, already used, or expired.
///
/// Mapped to `400 RESET_LINK_EXPIRED`.
public class ResetLinkExpiredException extends RuntimeException {

    public ResetLinkExpiredException(String message) {
        super(message);
    }
}
