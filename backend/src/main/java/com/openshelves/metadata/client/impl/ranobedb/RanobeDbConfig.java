package com.openshelves.metadata.client.impl.ranobedb;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/// Configuration class for the RanobeDB client.
@Configuration
public class RanobeDbConfig {

    /// Custom HTTP client configuration for the RanobeDB service (light novels).
    ///
    /// Configures connection and read/write timeouts, a connection pool,
    /// and adds the [RanobeDbInterceptor] to handle headers, rate limiting, and retries.
    @Bean
    @Qualifier("ranobeDb")
    public OkHttpClient ranobeDbHttpClient(OkHttpClient.Builder httpClientBuilder, RanobeDbInterceptor ranobeDbInterceptor) {
        return httpClientBuilder
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))     // Max 5 idle connections, 5 minutes keep-alive
            .addInterceptor(ranobeDbInterceptor)   // Add custom interceptor
            .build();
    }
}
