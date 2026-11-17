package com.openshelves.metadata.client.impl.comicvine;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/// Configuration class for the Comic Vine client.
@Configuration
public class ComicVineConfig {

    /// Custom HTTP client configuration for the Comic Vine service (comics/graphic novels).
    ///
    /// Configures connection and read/write timeouts, a connection pool,
    /// and adds the [ComicVineInterceptor] to handle headers, rate limiting, and retries.
    @Bean
    @Qualifier("comicVine")
    public OkHttpClient comicVineHttpClient(OkHttpClient.Builder httpClientBuilder, ComicVineInterceptor comicVineInterceptor) {
        return httpClientBuilder
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))     // Max 5 idle connections, 5 minutes keep-alive
            .addInterceptor(comicVineInterceptor)   // Add custom interceptor
            .build();
    }
}
