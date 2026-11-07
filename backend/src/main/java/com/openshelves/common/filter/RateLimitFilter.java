package com.openshelves.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.openshelves.common.filter.RateLimitProperties.Endpoint;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/// Limits how often one client IP can send requests, per minute, as configured in [RateLimitProperties].
/// Two kinds of limit:
///
/// - Per endpoint: each limit names an endpoint (method and exact path) and how many requests
///   one IP may make to it, signed in or not. For endpoints that check credentials or send email.
/// - Everything else that isn't signed in shares one, more generous, limit per IP, which only
///   catches floods. Signed-in requests and exempt paths (e.g. health checks) aren't limited.
///
/// Over a limit, the request gets `429 Too Many Requests` with a `Retry-After` header.
/// Must run after authentication, so it can tell whether the request is signed in.
///
/// Counts are kept in memory per app instance, in fixed one-minute windows. Expired windows are
/// dropped once a minute, so memory stays bounded by the clients seen in the last minute.
/// The IP is the request's remote address; behind a proxy, configure `server.forward-headers-strategy`.
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    /// One client's count on one endpoint, for the window that started at `startMillis`.
    @Value
    private static class Window {
        long startMillis;
        int count;
    }

    private final List<Endpoint> limits;

    /// Requests allowed per IP per minute for unauthenticated requests no endpoint limit covers.
    private final int unauthenticatedPerMinute;

    /// Path prefixes never limited, e.g. `/actuator/health`.
    private final List<String> exemptPathPrefixes;

    /// Keyed by `bucket|ip`, where the bucket is the endpoint, or `unauthenticated` for the shared limit.
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private final AtomicLong nextCleanupMillis = new AtomicLong();

    /// @param properties the limits, from `openshelves.rate-limit`
    public RateLimitFilter(RateLimitProperties properties) {
        this.limits = List.copyOf(properties.getEndpoints());
        this.unauthenticatedPerMinute = properties.getUnauthenticatedPerMinute() != null ? properties.getUnauthenticatedPerMinute() : Integer.MAX_VALUE;
        this.exemptPathPrefixes = List.copyOf(properties.getExemptPaths());
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return exemptPathPrefixes.stream().anyMatch(request.getRequestURI()::startsWith);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        // The endpoint's own limit; otherwise the shared one, for unauthenticated requests only (if set)
        Optional<Endpoint> endpointLimit = limitFor(request);
        if (endpointLimit.isEmpty() && (unauthenticatedPerMinute == Integer.MAX_VALUE || isAuthenticated())) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucket = endpointLimit.map(l -> l.getMethod().name() + " " + l.getPath()).orElse("unauthenticated");
        int perMinute = endpointLimit.map(Endpoint::getPerMinute).orElse(unauthenticatedPerMinute);
        String key = bucket + "|" + request.getRemoteAddr();
        long now = System.currentTimeMillis();

        // Count this request in the client's current window, starting a new window when the last one has passed
        Window window = windows.compute(key, (_, current) ->
            current == null || now - current.getStartMillis() >= WINDOW.toMillis()
                ? new Window(now, 1)
                : new Window(current.getStartMillis(), current.getCount() + 1));

        dropExpiredWindows(now);

        if (window.getCount() > perMinute) {
            long retryAfterSeconds = Math.max(1, Math.ceilDiv(window.getStartMillis() + WINDOW.toMillis() - now, 1000));
            log.debug("Rate limited [bucket={}, ip={}]", bucket, request.getRemoteAddr());

            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many requests. Please wait a minute and try again.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /// Whether the request is signed in; anonymous authentication doesn't count.
    private static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }

    /// The limit for this request's endpoint, if it has one.
    private Optional<Endpoint> limitFor(HttpServletRequest request) {
        return limits.stream()
            .filter(limit -> limit.getMethod().matches(request.getMethod()) && limit.getPath().equals(request.getRequestURI()))
            .findFirst();
    }

    /// Drops windows that have ended, at most once per window length.
    private void dropExpiredWindows(long now) {
        long next = nextCleanupMillis.get();
        if (now >= next && nextCleanupMillis.compareAndSet(next, now + WINDOW.toMillis())) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().getStartMillis() >= WINDOW.toMillis());
        }
    }
}
