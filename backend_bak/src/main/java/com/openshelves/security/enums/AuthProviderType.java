package com.openshelves.security.enums;

/// The protocol an identity provider speaks.
///
/// Mirrors the `lu_auth_provider_types` lookup table, used by `auth_providers.type`.
/// Passwords aren't a provider, so they have no entry here.
public enum AuthProviderType {

    /// SAML 2.0 identity provider, e.g. Shibboleth.
    SAML,

    /// OpenID Connect identity provider, e.g. Google.
    OIDC
}
