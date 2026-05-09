package com.openshelves.services.metadata.openlibrary;

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

/**
 * OkHttp Interceptor for the Open Library API.
 * Handles rate limiting, retry logic, and adding standard headers (e.g., User-Agent)
 * to comply with Open Library's usage policies.
 */
@Slf4j
@Component
public class OpenLibraryInterceptor implements Interceptor {

    private static final String USER_AGENT = "OpenShelves Metadata Service (arpan.mahanty.007@gmail.com)";


    /// Rate Limits

    // API endpoints: 180 req/min = 3 req/sec
    private final RateLimiter apiLimiter = RateLimiter.of("api", RateLimiterConfig.custom()
        .limitForPeriod(180)
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .build());

    // Cover images: 400 req/min = ~6.67 req/sec
    private final RateLimiter coverLimiter = RateLimiter.of("cover", RateLimiterConfig.custom()
        .limitForPeriod(400)
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .build());


    /// Retry Configuration

    private final Retry retry = Retry.of("openLibrary", RetryConfig.custom()
        .maxAttempts(3)
        .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))
        .retryOnResult(response ->
            ((Response) response).code() == 429 || ((Response) response).code() == 403)

        .consumeResultBeforeRetryAttempt((numTries, response) -> {
            int statusCode = ((Response) response).code();
            log.warn("Received HTTP status code {} from Open Library API. Retrying ... #{}", statusCode, numTries);

            // Close response to prevent resource leaks
            ((Response) response).close();
        })
        .build());


    /// Interceptor Method

    @Override
    public @NonNull Response intercept(@NonNull Chain chain) throws IOException {
        // Add User-Agent header to all requests
        Request request = chain.request().newBuilder()
            .addHeader("User-Agent", USER_AGENT)
            .build();

        // Initialize Supplier Chain for Resilience4j
        CheckedSupplier<Response> requestHandler = () -> chain.proceed(request);

        // Apply Rate Limiters
        RateLimiter rateLimiter = getRateLimiter(request);
        requestHandler = RateLimiter.decorateCheckedSupplier(rateLimiter, requestHandler);

        // Apply Retry Logic
        requestHandler = Retry.decorateCheckedSupplier(retry, requestHandler);

        try {
            return requestHandler.get();
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            throw new IOException("Unexpected error occurred while making request to Open Library API", e);
        }
    }

    // Helper method to select appropriate Rate Limiter
    private RateLimiter getRateLimiter(Request request) {
        String host = request.url().host();

        if (host.equalsIgnoreCase("covers.openlibrary.org")) {
            return coverLimiter;    // cover image limiter
        }

        return apiLimiter;  // default to API limiter
    }
}
