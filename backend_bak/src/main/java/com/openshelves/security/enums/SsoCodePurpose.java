package com.openshelves.security.enums;

/// What a short-lived SSO code is for.
public enum SsoCodePurpose {

    /// Bridges the SSO callback and `POST /auth/sso/exchange`.
    LOGIN,

    /// Starts connecting an identity provider from settings.
    LINK
}
