package com.openshelves.metadata.client.impl.openlibrary;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/// Configuration class for the Open Library client.
@Configuration
public class OpenLibraryConfig {

    /// Custom HTTP client configuration for the Open Library service.
    ///
    /// Configures connection and read/write timeouts, a connection pool,
    /// and adds the [OpenLibraryInterceptor] to handle rate limiting and retries.
    @Bean
    @Qualifier("openLibrary")
    public OkHttpClient openLibraryHttpClient(OkHttpClient.Builder httpClientBuilder, OpenLibraryInterceptor openLibraryInterceptor) {
        return httpClientBuilder
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))     // Max 5 idle connections, 5 minutes keep-alive
            .addInterceptor(openLibraryInterceptor)   // Add custom interceptor
            .build();
    }
}
