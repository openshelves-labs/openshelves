package com.openshelves.security.sso;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/// Builds the frontend URLs the browser is sent back to at the end of an SSO round-trip.
///
/// Sign-in results go to `{frontend}/auth/callback`; connecting a provider goes back to
/// `{frontend}/settings/sign-in-methods`. Nothing in these URLs identifies the user.
@Component
public class SsoRedirects {

    private final String frontendUrl;

    public SsoRedirects(@Value("${openshelves.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    /// A successful sign-in: the one-time code to exchange, and where to go afterwards.
    public String signedIn(String code, String returnTo) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
            .path("/auth/callback")
            .queryParam("code", code)
            .queryParamIfPresent("returnTo", Optional.ofNullable(returnTo))
            .encode()
            .toUriString();
    }

    /// A failed sign-in, e.g. `IDENTITY_NOT_LINKED`.
    public String signInFailed(String error) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
            .path("/auth/callback")
            .queryParam("error", error)
            .encode()
            .toUriString();
    }

    /// A provider connected from settings.
    public String linked(String providerKey) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
            .path("/settings/sign-in-methods")
            .queryParam("linked", providerKey)
            .encode()
            .toUriString();
    }

    /// Connecting a provider failed, e.g. `IDENTITY_IN_USE`.
    public String linkFailed(String error) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
            .path("/settings/sign-in-methods")
            .queryParam("error", error)
            .encode()
            .toUriString();
    }

    /// Accepts only a path on the frontend (e.g. `/books/42`), so the redirect can't leave it.
    ///
    /// @return the path, or `null` if it's missing or not a plain path
    public static String safeReturnTo(String returnTo) {
        boolean plainPath = returnTo != null
            && returnTo.startsWith("/")
            && !returnTo.startsWith("//")
            && !returnTo.contains("\\")
            && returnTo.length() <= 2048;

        return plainPath ? returnTo : null;
    }
}
