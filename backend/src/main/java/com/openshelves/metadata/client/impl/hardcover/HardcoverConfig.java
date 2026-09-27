package com.openshelves.metadata.client.impl.hardcover;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/// Configuration class for the Hardcover client.
@Configuration
public class HardcoverConfig {

    /// Custom HTTP client configuration for the Hardcover service.
    ///
    /// Configures connection and read/write timeouts, a connection pool,
    /// and adds the [HardcoverInterceptor] to handle rate limiting, and retries.
    @Bean
    @Qualifier("hardcover")
    public OkHttpClient hardcoverHttpClient(OkHttpClient.Builder httpClientBuilder, HardcoverInterceptor hardcoverInterceptor) {
        return httpClientBuilder
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))     // Max 5 idle connections, 5 minutes keep-alive
            .addInterceptor(hardcoverInterceptor)   // Add custom interceptor
            .build();
    }
}
