package com.openshelves.common.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/// Checks frontend URLs the API sends users to, e.g. after SSO or in a password reset email.
///
/// A URL is accepted only if it's an absolute http(s) URL whose origin the API's CORS rules allow:
/// a frontend that may call the API may also receive its links and one-time codes. So CORS must list
/// only our own frontends.
@Component
@RequiredArgsConstructor
public class FrontendUrlValidator {

    private final CorsConfigurationSource corsConfigurationSource;

    /// Rejects a URL that isn't a page on an allowed frontend origin.
    ///
    /// @param url the URL to check
    /// @param parameterName the request parameter it came from, for the error message
    /// @param request the current request, whose CORS rules apply
    /// @throws ResponseStatusException `400` if the URL isn't allowed
    public void check(String url, String parameterName, HttpServletRequest request) {
        URI uri = parseOrNull(url);
        CorsConfiguration cors = corsConfigurationSource.getCorsConfiguration(request);

        boolean allowed = uri != null
            && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
            && uri.getHost() != null
            && uri.getRawUserInfo() == null
            && cors != null
            && cors.checkOrigin(originOf(uri)) != null;

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, parameterName + " must be a page on an allowed frontend origin.");
        }
    }

    private static URI parseOrNull(String url) {
        try {
            return url != null ? new URI(url) : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /// The origin of a URL, e.g. `https://app.uni.edu` or `http://localhost:3000`.
    private static String originOf(URI uri) {
        String origin = uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT);
        return uri.getPort() != -1 ? origin + ":" + uri.getPort() : origin;
    }
}
