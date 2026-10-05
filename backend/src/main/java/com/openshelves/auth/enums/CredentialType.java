package com.openshelves.auth.enums;

/// Represents the different types of credentials a user can have in the OpenShelves system.
///
/// Used to classify the authentication method associated with a user's account, such as local password-based login,
/// SAML identity provider login, or OpenID Connect login.
public enum CredentialType {

    /// Local id/password login; backed by `password_hash`.
    PASSWORD,

    /// SAML identity-provider login; backed by `provider_key`.
    SAML,

    /// OpenID Connect login; backed by `provider_key`.
    OIDC
}
