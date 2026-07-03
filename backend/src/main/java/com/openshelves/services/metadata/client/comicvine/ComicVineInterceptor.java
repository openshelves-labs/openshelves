package com.openshelves.services.metadata.client.comicvine;

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
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/// OkHttp Interceptor for the Comic Vine API.
///
/// Adds the required `User-Agent` header, applies a conservative rate limit, and retries
/// on Comic Vine's rate-limit status codes (`420`/`429`).
@Slf4j
@Component
public class ComicVineInterceptor implements Interceptor {

    private static final String USER_AGENT = "OpenShelves Metadata Service (arpan.mahanty.007@gmail.com)";


    /// Rate Limits

    // Comic Vine does not publish a fixed quota, but is known to throttle aggressively on
    // bursts; 30 req/min (1 every 2s) matches the interval Grimmory settled on empirically.
    private final RateLimiter apiLimiter = RateLimiter.of("api", RateLimiterConfig.custom()
        .limitForPeriod(30)
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .build());


    /// Retry Configuration

    private final Retry retry = Retry.of("comicVine", RetryConfig.custom()
        .maxAttempts(3)
        .intervalFunction(IntervalFunction.ofExponentialBackoff(2000, 2))
        .retryOnResult(response -> {
            int code = ((Response) response).code();
            return code == 429 || code == 420;   // Comic Vine uses 420 for rate-limit, not just 429
        })
        .consumeResultBeforeRetryAttempt((numTries, response) -> {
            log.warn("Comic Vine API rate limited (status={}). Retrying ... #{}", ((Response) response).code(), numTries);

            // Close response to prevent resource leaks
            ((Response) response).close();
        })
        .build());


    /// Interceptor Method

    /// Intercepts the HTTP request to apply headers, rate limiting, and retry logic.
    ///
    /// @param chain the OkHttp interceptor chain
    /// @return the HTTP response
    /// @throws IOException if a network error occurs or if the resilience chain fails
    @Override
    public @NonNull Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request().newBuilder()
            .addHeader("User-Agent", USER_AGENT)
            .build();

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
            throw new IOException("Unexpected error occurred while making request to Comic Vine API", e);
        }
    }
}
