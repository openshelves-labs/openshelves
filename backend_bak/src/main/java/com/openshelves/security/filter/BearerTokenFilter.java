package com.openshelves.security.filter;

import com.openshelves.security.services.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/// Authenticates requests that carry an `Authorization: Bearer <token>` header.
///
/// When the token is valid, the resulting [Authentication] is stored in the
/// [SecurityContextHolder] for the rest of the request.
///
/// Requests without a bearer token, or with an invalid one, continue unauthenticated.
/// The authorization rules then reject them with a 401 wherever authentication is required.
@RequiredArgsConstructor
public class BearerTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Read the bearer token from the Authorization header, if any
        Optional<String> authToken = extractBearerToken(request);

        if (authToken.isPresent()) {
            // Resolve the token to an authenticated user; empty if unknown, expired or revoked
            Optional<Authentication> authentication = authService.authenticateSessionToken(authToken.get());

            if (authentication.isPresent()) {
                // Store the authentication in the SecurityContext for the rest of the request
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication.get());
                SecurityContextHolder.setContext(context);
            }
        }

        // Always continue the chain; unauthenticated requests are rejected later
        // by the authorization rules where authentication is required
        filterChain.doFilter(request, response);
    }


    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /// Extracts the bearer token from the `Authorization` header of the request.
    public static Optional<String> extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Check if the Authorization header is present and starts with "Bearer "
        if (authHeader != null && authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();

            if (StringUtils.isNotBlank(token)) {
                return Optional.of(token);
            }
        }

        return Optional.empty();
    }
}
