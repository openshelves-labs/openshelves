package com.openshelves.security.services;

import com.openshelves.common.util.CipherUtils;
import com.openshelves.security.enums.AuthProviderType;
import com.openshelves.security.model.SsoContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestRepository;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/// Keeps all state of the identity-provider round-trip in short-lived, encrypted cookies,
/// in place of an HTTP session (the API has none). Two cookies:
///
/// - [#CONTEXT]: our own [SsoContext], saved at `start`, taken at the callback.
/// - [#REQUEST]: Spring Security's pending request (OIDC state, nonce and PKCE verifier, or the SAML request ID).
///   This class is Spring's store for it, for both protocols.
///
/// The cookies are `HttpOnly`, `Secure`, scoped to the SSO paths of the API, and live for minutes.
/// Their content is Java-serialized and then encrypted with AES-GCM, so it can't be read or forged.
///
/// `SameSite` depends on how the provider returns: OIDC redirects back with a GET, so `Lax` is enough;
/// SAML posts back from the provider's site, so the cookie must be `None` to come along.
@Slf4j
@Component
@RequiredArgsConstructor
public class SsoCookieStore implements
    AuthorizationRequestRepository<OAuth2AuthorizationRequest>,
    Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest> {

    /// Our own [SsoContext].
    private static final String CONTEXT = "os_sso_ctx";

    /// Spring Security's pending request: OIDC state, nonce and PKCE verifier, or the SAML request ID.
    private static final String REQUEST = "os_sso_req";

    private static final String PATH = "/api/v1/auth/sso";
    private static final Duration MAX_AGE = Duration.ofMinutes(10);

    private final CipherUtils cipher;


    // -----------------------------------------------------------------------
    // Our Context
    // -----------------------------------------------------------------------

    /// Saves our context at `start`, for the callback to read.
    ///
    /// @param type the provider's protocol, which decides `SameSite`
    public void saveContext(HttpServletResponse response, SsoContext context, AuthProviderType type) {
        write(response, CONTEXT, context, type == AuthProviderType.SAML ? "None" : "Lax");
    }

    /// Reads our context at the callback and clears both cookies; each round-trip uses them once.
    ///
    /// @return the context, or empty if the round-trip didn't start at `start` or took too long
    public Optional<SsoContext> takeContext(HttpServletRequest request, HttpServletResponse response) {
        Optional<SsoContext> context = read(request, CONTEXT, SsoContext.class);
        clear(response, CONTEXT);
        clear(response, REQUEST);
        return context;
    }


    // -----------------------------------------------------------------------
    // Spring's Pending Request: OIDC
    // -----------------------------------------------------------------------

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return read(request, REQUEST, OAuth2AuthorizationRequest.class).orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            clear(response, REQUEST);
        } else {
            write(response, REQUEST, authorizationRequest, "Lax");
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        clear(response, REQUEST);
        return authorizationRequest;
    }


    // -----------------------------------------------------------------------
    // Spring's Pending Request: SAML
    // -----------------------------------------------------------------------

    @Override
    public AbstractSaml2AuthenticationRequest loadAuthenticationRequest(HttpServletRequest request) {
        return read(request, REQUEST, AbstractSaml2AuthenticationRequest.class).orElse(null);
    }

    @Override
    public void saveAuthenticationRequest(AbstractSaml2AuthenticationRequest authenticationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authenticationRequest == null) {
            clear(response, REQUEST);
        } else {
            write(response, REQUEST, authenticationRequest, "None");
        }
    }

    @Override
    public AbstractSaml2AuthenticationRequest removeAuthenticationRequest(HttpServletRequest request, HttpServletResponse response) {
        AbstractSaml2AuthenticationRequest authenticationRequest = loadAuthenticationRequest(request);
        clear(response, REQUEST);
        return authenticationRequest;
    }


    // -----------------------------------------------------------------------
    // Read & Write
    // -----------------------------------------------------------------------

    /// Stores a value in an encrypted cookie.
    ///
    /// @param sameSite `Lax` for OIDC, `None` for SAML
    private void write(HttpServletResponse response, String name, Serializable value, String sameSite) {
        String encrypted = cipher.encrypt(serialize(value));
        addCookie(response, name, encrypted, MAX_AGE, sameSite);
    }

    /// Reads a value from an encrypted cookie.
    ///
    /// @return the value, or empty if the cookie is missing, expired, tampered with or of another type
    private <T> Optional<T> read(HttpServletRequest request, String name, Class<T> type) {
        Optional<String> raw = Optional.ofNullable(request.getCookies()).stream()
            .flatMap(Arrays::stream)
            .filter(cookie -> cookie.getName().equals(name))
            .map(Cookie::getValue)
            .findFirst();

        try {
            return raw.map(cipher::decrypt).map(this::deserialize).filter(type::isInstance).map(type::cast);
        } catch (RuntimeException e) {
            log.debug("Ignoring unreadable SSO cookie [name={}]", name, e);
            return Optional.empty();
        }
    }

    /// Deletes a cookie.
    private void clear(HttpServletResponse response, String name) {
        addCookie(response, name, "", Duration.ZERO, "Lax");
    }


    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge, String sameSite) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
            .path(PATH)
            .httpOnly(true)
            .secure(true)
            .sameSite(sameSite)
            .maxAge(maxAge)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private byte[] serialize(Serializable value) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /// Only ever called on data that decrypted successfully, i.e. data this application wrote.
    private Object deserialize(byte[] data) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException("Unreadable SSO cookie", e);
        }
    }
}
