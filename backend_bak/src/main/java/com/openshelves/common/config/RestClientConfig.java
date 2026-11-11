package com.openshelves.common.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/// Configuration class for creating and managing HTTP client beans.
@Configuration
public class RestClientConfig {

    private static final OkHttpClient baseClient = new OkHttpClient();

    /// HTTP client with default configuration, for general use across the application.
    @Bean
    @Primary
    public OkHttpClient httpClient() {
        return baseClient;
    }

    /// HTTP client builder bean, for creating customized HTTP clients.
    @Bean
    public OkHttpClient.Builder httpClientBuilder() {
        return baseClient.newBuilder();
    }
}
