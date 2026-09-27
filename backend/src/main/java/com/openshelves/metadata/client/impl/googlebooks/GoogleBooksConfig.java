package com.openshelves.metadata.client.impl.googlebooks;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/// Configuration class for the Google Books client.
@Configuration
public class GoogleBooksConfig {

    /// Custom HTTP client configuration for the Google Books service.
    ///
    /// Configures connection and read/write timeouts, a connection pool,
    /// and adds the [GoogleBooksInterceptor] to handle rate limiting and retries.
    @Bean
    @Qualifier("googleBooks")
    public OkHttpClient googleBooksHttpClient(OkHttpClient.Builder httpClientBuilder, GoogleBooksInterceptor googleBooksInterceptor) {
        return httpClientBuilder
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))     // Max 5 idle connections, 5 minutes keep-alive
            .addInterceptor(googleBooksInterceptor)   // Add custom interceptor
            .build();
    }
}
