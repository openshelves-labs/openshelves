package com.openshelves.security.sso;

import com.openshelves.security.services.SecretCipher;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/// Reads and writes the short-lived, encrypted cookies that carry state across the
/// identity-provider round-trip, in place of an HTTP session.
///
/// The cookies are `HttpOnly`, `Secure`, scoped to the SSO paths of the API, and live for minutes.
/// Their content is Java-serialized and then encrypted with AES-GCM, so it can't be read or forged.
///
/// `SameSite` depends on how the provider returns: OIDC redirects back with a GET, so `Lax` is enough;
/// SAML posts back from the provider's site, so the cookie must be `None` to come along.
@Slf4j
@Component
@RequiredArgsConstructor
public class SsoCookies {

    /// Our own context: persistent, returnTo, linking user.
    public static final String CONTEXT = "os_sso_ctx";

    /// Spring Security's pending request: OIDC state, nonce and PKCE verifier, or the SAML request ID.
    public static final String REQUEST = "os_sso_req";

    private static final String PATH = "/api/v1/auth/sso";
    private static final Duration MAX_AGE = Duration.ofMinutes(10);

    private final SecretCipher cipher;


    // -----------------------------------------------------------------------
    // Read & Write
    // -----------------------------------------------------------------------

    /// Stores a value in an encrypted cookie.
    ///
    /// @param sameSite `Lax` for OIDC, `None` for SAML
    public void write(HttpServletResponse response, String name, Serializable value, String sameSite) {
        String encrypted = cipher.encrypt(serialize(value));
        addCookie(response, name, encrypted, MAX_AGE, sameSite);
    }

    /// Reads a value from an encrypted cookie.
    ///
    /// @return the value, or empty if the cookie is missing, expired, tampered with or of another type
    public <T> Optional<T> read(HttpServletRequest request, String name, Class<T> type) {
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
    public void clear(HttpServletResponse response, String name) {
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
