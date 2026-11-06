package com.openshelves.security.controller;

import com.openshelves.common.exception.ApiException;
import com.openshelves.security.entity.AuthProviderEntity;
import com.openshelves.security.enums.AuthProviderType;
import com.openshelves.security.model.DeviceInfo;
import com.openshelves.security.model.ExchangeRequest;
import com.openshelves.security.model.LoginResponse;
import com.openshelves.security.model.ProviderResponse;
import com.openshelves.security.services.SsoService;
import com.openshelves.security.sso.SsoContext;
import com.openshelves.security.sso.SsoCookies;
import com.openshelves.security.sso.SsoRedirects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/// REST controller for signing in with an SSO identity provider.
///
/// Only `start` and `exchange` are ours; the redirect to the provider and its callback
/// (`oidc-callback`, `saml-callback`) and SAML metadata are handled by Spring Security filters.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class SsoController {

    /// Spring Security's own redirect endpoints, which build the request to the provider.
    private static final String OIDC_AUTHORIZE_PATH = "/api/v1/auth/sso/oidc-authorize/";
    private static final String SAML_AUTHORIZE_PATH = "/api/v1/auth/sso/saml-authorize/";

    private final SsoService ssoService;
    private final SsoCookies cookies;
    private final SsoRedirects redirects;

    /// Lists the providers for the sign-in page's dropdown.
    @GetMapping("/providers")
    public List<ProviderResponse> listProviders() {
        return ssoService.listProviders();
    }

    /// Starts a round-trip to the identity provider, for signing in or for connecting the provider.
    ///
    /// Remembers our context in an encrypted cookie, then hands over to Spring Security's
    /// redirect endpoint for the provider's protocol. Reached by browser navigation, not `fetch`.
    ///
    /// @param provider the provider key
    /// @param persistent whether to open a long-lived ("remember me") session
    /// @param returnTo frontend path to open after sign-in; ignored unless it's a plain path
    /// @param link one-time code from `POST /auth/identities/{provider}`, when connecting the provider
    @GetMapping("/sso/{provider}/start")
    public void start(@PathVariable String provider,
                      @RequestParam(defaultValue = "false") boolean persistent,
                      @RequestParam(required = false) String returnTo,
                      @RequestParam(required = false) String link,
                      HttpServletRequest request,
                      HttpServletResponse response) throws IOException {

        AuthProviderEntity authProvider = ssoService.getEnabledProvider(provider);

        // Connecting a provider: the link code says which signed-in user started it
        Long linkUserId = null;
        if (link != null) {
            try {
                linkUserId = ssoService.consumeLinkCode(authProvider.getKey(), link);
            } catch (ApiException e) {
                response.sendRedirect(redirects.linkFailed(e.getCode()));
                return;
            }
        }

        boolean saml = authProvider.getType() == AuthProviderType.SAML;

        SsoContext context = new SsoContext(authProvider.getKey(), persistent, SsoRedirects.safeReturnTo(returnTo), linkUserId);
        cookies.write(response, SsoCookies.CONTEXT, context, saml ? "None" : "Lax");

        String authorizePath = (saml ? SAML_AUTHORIZE_PATH : OIDC_AUTHORIZE_PATH)
            + UriUtils.encodePathSegment(authProvider.getKey(), StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + authorizePath);
    }

    /// Swaps the one-time code from the SSO callback for a session token.
    @PostMapping("/sso/exchange")
    public LoginResponse exchangeCode(@Valid @RequestBody ExchangeRequest request, HttpServletRequest httpRequest) {
        return ssoService.exchangeCode(request.getCode(), DeviceInfo.from(httpRequest));
    }
}
