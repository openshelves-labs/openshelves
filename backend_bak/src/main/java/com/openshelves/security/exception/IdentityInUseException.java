package com.openshelves.security.exception;

import org.springframework.security.core.AuthenticationException;

/// The SSO identity being connected is already linked to a different user.
///
/// Thrown at the SSO callback when connecting a provider.
public class IdentityInUseException extends AuthenticationException {

    public IdentityInUseException(String message) {
        super(message);
    }
}
