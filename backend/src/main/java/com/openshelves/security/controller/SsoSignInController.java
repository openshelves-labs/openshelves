package com.openshelves.security.controller;

import com.openshelves.common.util.FrontendUrlValidator;
import com.openshelves.security.entity.AuthProviderEntity;
import com.openshelves.security.enums.AuthProviderType;
import com.openshelves.security.exception.LinkExpiredException;
import com.openshelves.security.model.DeviceInfo;
import com.openshelves.security.model.ExchangeRequest;
import com.openshelves.security.model.LoginResponse;
import com.openshelves.security.model.ProviderResponse;
import com.openshelves.security.model.SsoContext;
import com.openshelves.security.services.SsoCallbackHandler;
import com.openshelves.security.services.SsoCookieStore;
import com.openshelves.security.services.SsoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/// REST controller for signing in with an SSO identity provider.
///
/// Only `start` and `exchange` are ours; the redirect to the provider and its callback
/// (`oidc-callback`, `saml-callback`) and SAML metadata are handled by Spring Security filters.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/sso")
public class SsoSignInController {

    /// Spring Security's own redirect endpoints, which build the request to the provider.
    private static final String OIDC_AUTHORIZE_PATH = "/api/v1/auth/sso/oidc-authorize/";
    private static final String SAML_AUTHORIZE_PATH = "/api/v1/auth/sso/saml-authorize/";

    /// An S256 PKCE challenge: 32 bytes of SHA-256 as URL-safe Base64 without padding.
    private static final Pattern PKCE_CHALLENGE = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final SsoService ssoService;
    private final SsoCookieStore cookieStore;

    private final FrontendUrlValidator frontendUrlValidator;

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
    /// @param frontendRedirectUrl the frontend page to come back to, e.g. `https://app.uni.edu/auth/callback`;
    ///                 its origin must be allowed by CORS
    /// @param codeChallenge PKCE challenge, `BASE64URL(SHA-256(verifier))`; required when signing in.
    ///                      The exchange must present the verifier, so an intercepted code is useless.
    /// @param link one-time code from `POST /auth/identities/{provider}`, when connecting the provider
    /// @throws ResponseStatusException `400` if `frontendRedirectUrl`'s origin isn't allowed by CORS, `404` if the provider is unknown or disabled
    @GetMapping("/{provider}/start")
    public void start(@PathVariable String provider,
                      @RequestParam(defaultValue = "false") boolean persistent,
                      @RequestParam String frontendRedirectUrl,
                      @RequestParam(required = false) String codeChallenge,
                      @RequestParam(required = false) String link,
                      HttpServletRequest request,
                      HttpServletResponse response) throws IOException {

        // Checked first: it's where every later outcome, including errors, is sent
        frontendUrlValidator.check(frontendRedirectUrl, "frontendRedirectUrl", request);

        // Signing in ends in a code exchange, which must be bound to this client
        if (link == null && (codeChallenge == null || !PKCE_CHALLENGE.matcher(codeChallenge).matches())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "codeChallenge is required: BASE64URL(SHA-256(verifier)).");
        }

        AuthProviderEntity authProvider = ssoService.getEnabledProvider(provider);

        // Connecting a provider: the link code says which signed-in user started it
        Long linkUserId = null;
        if (link != null) {
            try {
                linkUserId = ssoService.consumeLinkCode(authProvider.getKey(), link);
            } catch (LinkExpiredException e) {
                SsoCallbackHandler.redirect(response, frontendRedirectUrl, "error", SsoCallbackHandler.errorCode(e));
                return;
            }
        }

        SsoContext context = SsoContext.builder()
            .withProviderKey(authProvider.getKey())
            .withPersistent(persistent)
            .withFrontendRedirectUrl(frontendRedirectUrl)
            .withLinkUserId(linkUserId)
            .withCodeChallenge(linkUserId == null ? codeChallenge : null)
            .build();
        cookieStore.saveContext(response, context, authProvider.getType());

        String authorizePath = (authProvider.getType() == AuthProviderType.SAML ? SAML_AUTHORIZE_PATH : OIDC_AUTHORIZE_PATH)
            + UriUtils.encodePathSegment(authProvider.getKey(), StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + authorizePath);
    }

    /// Swaps the one-time code from the SSO callback for a session token.
    @PostMapping("/exchange")
    public LoginResponse exchangeCode(@Valid @RequestBody ExchangeRequest request, HttpServletRequest httpRequest) {
        return ssoService.exchangeCode(request.getCode(), request.getCodeVerifier(), DeviceInfo.from(httpRequest));
    }
}
