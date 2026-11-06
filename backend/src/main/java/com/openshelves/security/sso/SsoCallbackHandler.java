package com.openshelves.security.sso;

import com.openshelves.common.exception.ApiException;
import com.openshelves.security.entity.AuthProviderEntity;
import com.openshelves.security.repository.AuthProviderRepository;
import com.openshelves.security.services.SsoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/// Finishes an SSO round-trip once Spring Security has verified the provider's response
/// (OIDC ID token or SAML assertion), and sends the browser back to the frontend.
///
/// - Signing in: issues a one-time code for `POST /auth/sso/exchange`.
/// - Connecting a provider: links the identity to the user who started it.
///
/// Every outcome, including failures, is a redirect: the browser is mid-navigation, not making an API call.
@Slf4j
@Component
@RequiredArgsConstructor
public class SsoCallbackHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    /// Generic error for a failed round-trip: cancelled at the provider, invalid response, expired state.
    private static final String SSO_FAILED = "SSO_FAILED";

    private final SsoService ssoService;
    private final SsoCookies cookies;
    private final SsoRedirects redirects;
    private final AuthProviderRepository providerRepository;

    /// What the verified response says about the user.
    private record VerifiedIdentity(String providerKey, String subject, Map<String, Object> metadata) {
    }


    // -----------------------------------------------------------------------
    // Success
    // -----------------------------------------------------------------------

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        Optional<SsoContext> context = takeContext(request, response);

        // No context means the round-trip didn't start at /start, or took too long
        if (context.isEmpty()) {
            response.sendRedirect(redirects.signInFailed(SSO_FAILED));
            return;
        }

        try {
            String providerKey = registrationIdOf(authentication);
            if (!providerKey.equals(context.get().providerKey())) {
                throw new ApiException(null, SSO_FAILED, "Provider doesn't match the one the sign-in started with");
            }

            // The provider's issuer anchors the identity; the response was verified against this provider
            AuthProviderEntity provider = providerRepository.findByKey(providerKey)
                .orElseThrow(() -> new ApiException(null, SSO_FAILED, "Provider no longer exists"));
            VerifiedIdentity identity = identityOf(authentication, provider);

            if (context.get().isLink()) {
                ssoService.completeLink(context.get().linkUserId(), provider.getIssuer(), identity.subject(), identity.metadata());
                response.sendRedirect(redirects.linked(providerKey));
            } else {
                String code = ssoService.completeLogin(providerKey, provider.getIssuer(), identity.subject(), context.get().persistent());
                response.sendRedirect(redirects.signedIn(code, context.get().returnTo()));
            }
        } catch (ApiException e) {
            log.debug("SSO refused [code={}]: {}", e.getCode(), e.getMessage());
            redirectFailure(response, context.get(), e.getCode());
        }
    }


    // -----------------------------------------------------------------------
    // Failure
    // -----------------------------------------------------------------------

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        log.debug("SSO round-trip failed: {}", exception.getMessage());

        Optional<SsoContext> context = takeContext(request, response);
        if (context.isEmpty()) {
            response.sendRedirect(redirects.signInFailed(SSO_FAILED));
            return;
        }

        redirectFailure(response, context.get(), SSO_FAILED);
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    /// Reads our context cookie and clears both SSO cookies; each round-trip uses them once.
    private Optional<SsoContext> takeContext(HttpServletRequest request, HttpServletResponse response) {
        Optional<SsoContext> context = cookies.read(request, SsoCookies.CONTEXT, SsoContext.class);
        cookies.clear(response, SsoCookies.CONTEXT);
        cookies.clear(response, SsoCookies.REQUEST);
        return context;
    }

    private void redirectFailure(HttpServletResponse response, SsoContext context, String error) throws IOException {
        response.sendRedirect(context.isLink() ? redirects.linkFailed(error) : redirects.signInFailed(error));
    }

    /// The provider key the response was verified for.
    private String registrationIdOf(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oidc) {
            return oidc.getAuthorizedClientRegistrationId();
        }
        if (authentication.getPrincipal() instanceof Saml2AuthenticatedPrincipal saml) {
            return saml.getRelyingPartyRegistrationId();
        }
        throw new ApiException(null, SSO_FAILED, "Unexpected authentication type: " + authentication.getClass().getName());
    }

    /// Reads the user's ID at the provider, plus display hints, honouring `settings.attributeMapping`.
    ///
    /// Defaults: OIDC `sub`, `email`, `name`; SAML NameID, and no display hints.
    private VerifiedIdentity identityOf(Authentication authentication, AuthProviderEntity provider) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        String subject;

        if (authentication.getPrincipal() instanceof OidcUser user) {
            subject = provider.mappedAttribute("subject").map(user::getClaimAsString).orElse(user.getSubject());
            putIfPresent(metadata, "email", provider.mappedAttribute("email").map(user::getClaimAsString).orElse(user.getEmail()));
            putIfPresent(metadata, "name", provider.mappedAttribute("name").map(user::getClaimAsString).orElse(user.getFullName()));

        } else if (authentication.getPrincipal() instanceof Saml2AuthenticatedPrincipal user) {
            subject = provider.mappedAttribute("subject").map(name -> firstAttribute(user, name)).orElse(user.getName());
            putIfPresent(metadata, "email", provider.mappedAttribute("email").map(name -> firstAttribute(user, name)).orElse(null));
            putIfPresent(metadata, "name", provider.mappedAttribute("name").map(name -> firstAttribute(user, name)).orElse(null));

        } else {
            throw new ApiException(null, SSO_FAILED, "Unexpected principal type");
        }

        if (subject == null || subject.isBlank()) {
            throw new ApiException(null, SSO_FAILED, "Provider sent no subject");
        }

        return new VerifiedIdentity(provider.getKey(), subject, metadata.isEmpty() ? null : metadata);
    }

    private static String firstAttribute(Saml2AuthenticatedPrincipal user, String name) {
        Object value = user.getFirstAttribute(name);
        return value != null ? value.toString() : null;
    }

    private static void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}
