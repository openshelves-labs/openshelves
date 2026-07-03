package com.openshelves.services.metadata.client.ranobedb;

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

/// OkHttp Interceptor for the RanobeDB API.
///
/// Adds the required `User-Agent` header and applies a conservative rate limit with retry
/// on throttling/transient server errors. RanobeDB is a small community-run project without
/// a published quota, so the limit here is intentionally cautious.
@Slf4j
@Component
public class RanobeDbInterceptor implements Interceptor {

    private static final String USER_AGENT = "OpenShelves Metadata Service (arpan.mahanty.007@gmail.com)";


    /// Rate Limits

    // 2 req/sec — matches the interval other self-hosted clients (e.g. Grimmory) settle on
    // for this API out of courtesy to a community-run, non-commercial service.
    private final RateLimiter apiLimiter = RateLimiter.of("api", RateLimiterConfig.custom()
        .limitForPeriod(2)
        .limitRefreshPeriod(Duration.ofSeconds(1))
        .build());


    /// Retry Configuration

    private final Retry retry = Retry.of("ranobeDb", RetryConfig.custom()
        .maxAttempts(3)
        .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))
        .retryOnResult(response -> {
            int code = ((Response) response).code();
            return code == 429 || code == 503;
        })
        .consumeResultBeforeRetryAttempt((numTries, response) -> {
            log.warn("RanobeDB API returned status {}. Retrying ... #{}", ((Response) response).code(), numTries);

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
            throw new IOException("Unexpected error occurred while making request to RanobeDB API", e);
        }
    }
}
