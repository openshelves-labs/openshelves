package com.openshelves.security.exception;

import org.springframework.security.core.AuthenticationException;

/// No user has connected this SSO identity, so it can't be used to sign in.
///
/// Thrown at the SSO callback; the user must sign in another way and connect the provider from settings first.
public class IdentityNotLinkedException extends AuthenticationException {

    public IdentityNotLinkedException(String message) {
        super(message);
    }
}
