package com.openshelves.security.sso;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/// Keeps Spring Security's pending OIDC request (state, nonce, PKCE verifier) in an encrypted
/// cookie between the redirect to the provider and the callback, since the API has no HTTP session.
@Component
@RequiredArgsConstructor
public class CookieOidcRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    /// OIDC providers return with a top-level GET, which `Lax` cookies follow.
    private static final String SAME_SITE = "Lax";

    private final SsoCookies cookies;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return cookies.read(request, SsoCookies.REQUEST, OAuth2AuthorizationRequest.class).orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            cookies.clear(response, SsoCookies.REQUEST);
            return;
        }
        cookies.write(response, SsoCookies.REQUEST, authorizationRequest, SAME_SITE);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        cookies.clear(response, SsoCookies.REQUEST);
        return authorizationRequest;
    }
}
