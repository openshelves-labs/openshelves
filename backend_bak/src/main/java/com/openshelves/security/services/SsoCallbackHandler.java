package com.openshelves.security.services;

import com.openshelves.security.entity.AuthProviderEntity;
import com.openshelves.security.exception.IdentityInUseException;
import com.openshelves.security.exception.IdentityNotLinkedException;
import com.openshelves.security.exception.LinkExpiredException;
import com.openshelves.security.model.SsoContext;
import com.openshelves.security.repository.AuthProviderRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/// Finishes an SSO round-trip once Spring Security has verified the provider's response
/// (OIDC ID token or SAML assertion), and sends the browser back to the frontend page it came from
/// (`frontendRedirectUrl`, checked at start against the API's CORS rules).
///
/// - Signing in: issues a one-time code for `POST /auth/sso/exchange`.
/// - Connecting a provider: links the identity to the user who started it.
///
/// Every outcome, including failures, is a redirect: the browser is mid-navigation, not making an API call.
/// Failures are typed exceptions, mapped to the `?error=` code in one place ([#errorCode]).
@Slf4j
@Component
@RequiredArgsConstructor
public class SsoCallbackHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final SsoService ssoService;
    private final SsoCookieStore cookieStore;
    private final AuthProviderRepository providerRepository;

    /// What the verified response says about the user.
    @Value
    private static class VerifiedIdentity {
        String subject;
        Map<String, Object> metadata;
    }


    // -----------------------------------------------------------------------
    // Success
    // -----------------------------------------------------------------------

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        Optional<SsoContext> context = cookieStore.takeContext(request, response);

        // No context means the round-trip didn't start at /start, or took too long;
        // with no trusted page to go back to, answer here instead of redirecting
        if (context.isEmpty()) {
            rejectWithoutContext(response);
            return;
        }

        try {
            String providerKey = registrationIdOf(authentication);
            if (!providerKey.equals(context.get().getProviderKey())) {
                throw new BadCredentialsException("Provider doesn't match the one the sign-in started with");
            }

            // The provider's issuer anchors the identity; the response was verified against this provider
            AuthProviderEntity provider = providerRepository.findByKey(providerKey)
                .orElseThrow(() -> new BadCredentialsException("Provider no longer exists"));
            VerifiedIdentity identity = identityOf(authentication, provider);

            if (context.get().isLink()) {
                ssoService.completeLink(context.get().getLinkUserId(), provider.getIssuer(), identity.getSubject(), identity.getMetadata());
                redirect(response, context.get().getFrontendRedirectUrl(), "linked", providerKey);
            } else {
                String code = ssoService.completeLogin(providerKey, provider.getIssuer(), identity.getSubject(), context.get().isPersistent(), context.get().getCodeChallenge());
                redirect(response, context.get().getFrontendRedirectUrl(), "code", code);
            }
        } catch (AuthenticationException e) {
            log.debug("SSO refused: {}", e.getMessage());
            redirect(response, context.get().getFrontendRedirectUrl(), "error", errorCode(e));
        }
    }


    // -----------------------------------------------------------------------
    // Failure
    // -----------------------------------------------------------------------

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        log.debug("SSO round-trip failed: {}", exception.getMessage());

        Optional<SsoContext> context = cookieStore.takeContext(request, response);
        if (context.isEmpty()) {
            rejectWithoutContext(response);
            return;
        }

        redirect(response, context.get().getFrontendRedirectUrl(), "error", errorCode(exception));
    }


    // -----------------------------------------------------------------------
    // Error Codes
    // -----------------------------------------------------------------------

    /// Maps a failure to the `?error=` code the frontend branches on.
    ///
    /// @param exception the failure; `null` when there's nothing more specific
    /// @return the error code; `SSO_FAILED` for anything that isn't about the user's account or link,
    ///         e.g. cancelled at the provider or an invalid response
    public static String errorCode(AuthenticationException exception) {
        return switch (exception) {
            case IdentityNotLinkedException _ -> "IDENTITY_NOT_LINKED";
            case IdentityInUseException _ -> "IDENTITY_IN_USE";
            case LinkExpiredException _ -> "LINK_EXPIRED";
            case DisabledException _ -> "ACCOUNT_DISABLED";
            case null, default -> "SSO_FAILED";
        };
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    /// Sends the browser back to the frontend page, with one result parameter added,
    /// e.g. `?code=…`, `?error=IDENTITY_NOT_LINKED` or `?linked=google`.
    ///
    /// @param frontendRedirectUrl the frontend page, already checked against the allowed origins
    public static void redirect(HttpServletResponse response, String frontendRedirectUrl, String name, String value) throws IOException {
        String url = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
            .queryParam(name, UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8))
            .build(true)    // frontendRedirectUrl is already encoded; the new value is encoded above
            .toUriString();

        response.sendRedirect(url);
    }

    /// Answers a callback that has no context cookie, as problem+json through the error controller.
    private void rejectWithoutContext(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "This sign-in has expired or wasn't started here. Please try again.");
    }

    /// The provider key the response was verified for.
    private String registrationIdOf(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oidc) {
            return oidc.getAuthorizedClientRegistrationId();
        }
        if (authentication instanceof Saml2AssertionAuthentication saml) {
            return saml.getRelyingPartyRegistrationId();
        }
        throw new InternalAuthenticationServiceException("Unexpected authentication type: " + authentication.getClass().getName());
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

        } else if (authentication instanceof Saml2AssertionAuthentication saml
                && saml.getCredentials() instanceof Saml2ResponseAssertionAccessor assertion) {
            subject = provider.mappedAttribute("subject").map(name -> firstAttribute(assertion, name)).orElse(assertion.getNameId());
            putIfPresent(metadata, "email", provider.mappedAttribute("email").map(name -> firstAttribute(assertion, name)).orElse(null));
            putIfPresent(metadata, "name", provider.mappedAttribute("name").map(name -> firstAttribute(assertion, name)).orElse(null));

        } else {
            throw new InternalAuthenticationServiceException("Unexpected authentication type: " + authentication.getClass().getName());
        }

        if (subject == null || subject.isBlank()) {
            throw new BadCredentialsException("Provider sent no subject");
        }

        return new VerifiedIdentity(subject, metadata.isEmpty() ? null : metadata);
    }

    private static String firstAttribute(Saml2ResponseAssertionAccessor assertion, String name) {
        Object value = assertion.getFirstAttribute(name);
        return value != null ? value.toString() : null;
    }

    private static void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}
