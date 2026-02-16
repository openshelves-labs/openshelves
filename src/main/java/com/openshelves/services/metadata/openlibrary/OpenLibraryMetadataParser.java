package com.openshelves.services.metadata.openlibrary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenLibraryMetadataParser implements ApplicationRunner {

    @Qualifier("openLibrary")
    private final OkHttpClient client;

    @Override
    public void run(@NonNull ApplicationArguments args) throws Exception {
        log.info("OpenLibraryMetadataParser initialized with custom HTTP client configuration.");
        log.info("HTTP Client Config: connectTimeout={}s, readTimeout={}s, writeTimeout={}s",
            client.connectTimeoutMillis() / 1000,
            client.readTimeoutMillis() / 1000,
            client.writeTimeoutMillis() / 1000
        );
        log.info("Interceptors: {}", client.interceptors());
    }
}
