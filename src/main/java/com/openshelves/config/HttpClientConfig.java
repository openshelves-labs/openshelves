package com.openshelves.config;

import com.openshelves.services.metadata.openlibrary.OpenLibraryInterceptor;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
public class HttpClientConfig {

    private static final OkHttpClient httpClient = new OkHttpClient();

    /**
     * Default HTTP client configuration, to be used by all services.
     */
    @Bean
    @Primary
    public OkHttpClient defaultClient() {
        return httpClient;
    }

    /**
     * Custom HTTP client configuration for OpenLibrary service.
     */
    @Bean
    @Qualifier("openLibrary")
    public OkHttpClient openLibraryClient() {
        return httpClient.newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))     // Max 5 idle connections, 5 minutes keep-alive
            .addInterceptor(new OpenLibraryInterceptor())   // Add custom interceptor
            .build();
    }
}
