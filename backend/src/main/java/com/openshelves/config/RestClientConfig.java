package com.openshelves.config;

import com.openshelves.services.metadata.client.googlebooks.GoogleBooksInterceptor;
import com.openshelves.services.metadata.client.openlibrary.OpenLibraryInterceptor;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/// Configuration class for creating and managing HTTP client beans.
///
/// Defines customized [OkHttpClient] instances for various external service integrations.
@Configuration
public class RestClientConfig {

    private static final OkHttpClient httpClient = new OkHttpClient();

    /// Default HTTP client configuration, used as the primary bean for all services.
    @Bean
    @Primary
    public OkHttpClient defaultHttpClient() {
        return httpClient;
    }

    /// Custom HTTP client configuration for the Open Library service.
    ///
    /// Configures connection and read/write timeouts, a connection pool,
    /// and adds the [OpenLibraryInterceptor] to handle rate limiting and retries.
    @Bean
    @Qualifier("openLibrary")
    public OkHttpClient openLibraryHttpClient() {
        return httpClient.newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))     // Max 5 idle connections, 5 minutes keep-alive
            .addInterceptor(new OpenLibraryInterceptor())   // Add custom interceptor
            .build();
    }

    /// Custom HTTP client configuration for the Google Books service.
    ///
    /// Configures connection and read/write timeouts, a connection pool,
    /// and adds the [GoogleBooksInterceptor] to handle rate limiting and retries.
    @Bean
    @Qualifier("googleBooks")
    public OkHttpClient googleBooksHttpClient() {
        return httpClient.newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))     // Max 5 idle connections, 5 minutes keep-alive
            .addInterceptor(new GoogleBooksInterceptor())   // Add custom interceptor
            .build();
    }
}
