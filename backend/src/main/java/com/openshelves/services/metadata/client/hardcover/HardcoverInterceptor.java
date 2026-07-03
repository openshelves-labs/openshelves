package com.openshelves.services.metadata.client.hardcover;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/// OkHttp Interceptor for the Hardcover GraphQL API.
///
/// Injects the bearer token required by every request, and applies a conservative
/// rate limit plus retry-on-throttle logic. Hardcover's API does not publish a fixed
/// quota, so the limit here is deliberately cautious rather than tuned against a
/// documented ceiling.
@Slf4j
@Component
public class HardcoverInterceptor implements Interceptor {

    /// Rate Limits

    // Hardcover publishes no fixed quota; 40 req/min leaves comfortable headroom for a
    // single-tenant self-hosted deployment while avoiding the throttling Hardcover applies
    // to bursty callers.
    private final RateLimiter apiLimiter = RateLimiter.of("api", RateLimiterConfig.custom()
        .limitForPeriod(40)
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .build());


    /// Retry Configuration

    private final Retry retry = Retry.of("hardcover", RetryConfig.custom()
        .maxAttempts(3)
        .intervalFunction(IntervalFunction.ofExponentialBackoff(1500, 2))
        .retryOnResult(response -> ((Response) response).code() == 429)

        .consumeResultBeforeRetryAttempt((numTries, response) -> {
            log.warn("Hardcover API throttled (429). Retrying ... #{}", numTries);

            // Close response to prevent resource leaks
            ((Response) response).close();
        })
        .build());


    /// Interceptor Method

    /// Intercepts the HTTP request to add apply rate limiting/retry.
    ///
    /// @param chain the OkHttp interceptor chain
    /// @return the HTTP response
    /// @throws IOException if a network error occurs or if the resilience chain fails
    @Override
    public @NonNull Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();

        // Initialize Supplier Chain for Resilience4j
        CheckedSupplier<Response> requestHandler = () -> chain.proceed(request);

        // Apply Rate Limiter
        requestHandler = RateLimiter.decorateCheckedSupplier(apiLimiter, requestHandler);

        // Apply Retry Logic
        requestHandler = Retry.decorateCheckedSupplier(retry, requestHandler);

        try {
            return requestHandler.get();
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            throw new IOException("Unexpected error occurred while making request to Hardcover API", e);
        }
    }
}
