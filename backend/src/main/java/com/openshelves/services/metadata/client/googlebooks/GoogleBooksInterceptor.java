package com.openshelves.services.metadata.client.googlebooks;

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

/// OkHttp Interceptor for the Google Books API.
///
/// Handles rate limiting and retry logic to stay within Google Book's unauthenticated
/// usage quotas and to recover gracefully from transient upstream failures.
@Slf4j
@Component
public class GoogleBooksInterceptor implements Interceptor {

    /// Rate Limits

    // Google Books' default project quota is generous (~1000 req/day for unauthenticated
    // callers, higher with an API key), but bursts are throttled. 80 req/min keeps us
    // comfortably under that ceiling with margin for other consumers of the same key.
    private final RateLimiter apiLimiter = RateLimiter.of("api", RateLimiterConfig.custom()
        .limitForPeriod(80)
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .build());


    /// Retry Configuration

    private final Retry retry = Retry.of("googleBooks", RetryConfig.custom()
        .maxAttempts(3)
        .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))
        .retryOnResult(response ->
            ((Response) response).code() == 429 || ((Response) response).code() == 503)

        .consumeResultBeforeRetryAttempt((numTries, response) -> {
            int statusCode = ((Response) response).code();
            log.warn("Received HTTP status code {} from Google Books API. Retrying ... #{}", statusCode, numTries);

            // Close response to prevent resource leaks
            ((Response) response).close();
        })
        .build());


    /// Interceptor Method

    /// Intercepts the HTTP request to apply rate limiting and retry logic.
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
            throw new IOException("Unexpected error occurred while making request to Google Books API", e);
        }
    }
}
