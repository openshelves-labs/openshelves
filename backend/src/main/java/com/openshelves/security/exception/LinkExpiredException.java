package com.openshelves.security.exception;

import org.springframework.security.core.AuthenticationException;

/// The link code for connecting a provider is unknown, already used, expired, or for another provider.
///
/// Thrown at the start of the round-trip; the user has to start connecting again.
public class LinkExpiredException extends AuthenticationException {

    public LinkExpiredException(String message) {
        super(message);
    }
}
