package com.openshelves.security.sso;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestRepository;
import org.springframework.stereotype.Component;

/// Keeps Spring Security's pending SAML request (its ID, checked against the response's `InResponseTo`)
/// in an encrypted cookie between the redirect to the provider and the callback, since the API has no HTTP session.
@Component
@RequiredArgsConstructor
public class CookieSamlRequestRepository implements Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest> {

    /// SAML providers post back from their own site, which only `None` cookies follow.
    private static final String SAME_SITE = "None";

    private final SsoCookies cookies;

    @Override
    public AbstractSaml2AuthenticationRequest loadAuthenticationRequest(HttpServletRequest request) {
        return cookies.read(request, SsoCookies.REQUEST, AbstractSaml2AuthenticationRequest.class).orElse(null);
    }

    @Override
    public void saveAuthenticationRequest(AbstractSaml2AuthenticationRequest authenticationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authenticationRequest == null) {
            cookies.clear(response, SsoCookies.REQUEST);
            return;
        }
        cookies.write(response, SsoCookies.REQUEST, authenticationRequest, SAME_SITE);
    }

    @Override
    public AbstractSaml2AuthenticationRequest removeAuthenticationRequest(HttpServletRequest request, HttpServletResponse response) {
        AbstractSaml2AuthenticationRequest authenticationRequest = loadAuthenticationRequest(request);
        cookies.clear(response, SsoCookies.REQUEST);
        return authenticationRequest;
    }
}
