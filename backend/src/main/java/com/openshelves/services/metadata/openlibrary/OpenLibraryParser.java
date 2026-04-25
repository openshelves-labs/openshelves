package com.openshelves.services.metadata.openlibrary;

import com.openshelves.exception.ThirdPartyClientException;
import com.openshelves.model.dto.BookMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenLibraryParser {

    private static final String OPEN_LIBRARY_BASE_URL = "https://openlibrary.org";

    @Qualifier("openLibrary")
    private final OkHttpClient httpClient;

    private final ObjectMapper mapper;

    public void run() {
        // Search for a book by title
        HttpUrl url = parseAndGetNewURLBuilder("/search.json")
            .addQueryParameter("q", "Anne Frank")
            .addQueryParameter("fields", "key,editions")
            .addQueryParameter("page", String.valueOf(1))
            .addQueryParameter("limit", String.valueOf(100))
            .addQueryParameter("lang", "en")
            .build();

        Request request = new Request.Builder().url(url).build();

        Optional<String> response = performApiRequest(request);
        if (response.isEmpty()) {
            log.warn("No results found for query: Anne Frank");
            return;
        }

        String str = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(response.get()));
        log.info("Response: {}", str);
    }

    public Optional<BookMetadata> fetchBookMetadata(BookMetadata preview) throws IllegalArgumentException, ThirdPartyClientException {
        return Optional.empty();
    }

    // *************************************************************
    // Helper Methods
    // *************************************************************

    // Execute the HTTP request and return the response body as a String
    private Optional<String> performApiRequest(Request request) throws ThirdPartyClientException {
        try (Response response = httpClient.newCall(request).execute()) {
            log.debug("Open Library API response: {}", response);

            // Handle unexpected HTTP status codes
            if (!response.isSuccessful()) {
                return handleUnsuccessfulResponse(response);
            }

            // Return the response body as a String
            return Optional.of(response.body().string());
        } catch (IOException e) {
            throw new ThirdPartyClientException("Error occurred while making request to Open Library API", e);
        }
    }

    // Handle unsuccessful HTTP responses and return appropriate results or throw exceptions
    private Optional<String> handleUnsuccessfulResponse(Response response) throws ThirdPartyClientException {
        int statusCode = response.code();

        if (statusCode == 404) {       // No results found
            return Optional.empty();
        }

        throw new ThirdPartyClientException("Unexpected response from Open Library API: " + response);
    }

    // Helper method to parse endpoint URL and return a new url builder
    private HttpUrl.Builder parseAndGetNewURLBuilder(String endpoint) throws IllegalArgumentException {
        try {
            return HttpUrl.get(OPEN_LIBRARY_BASE_URL + endpoint).newBuilder();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Open Library endpoint: " + endpoint, e);
        }
    }

    // Validate parameters are null or empty
    private void validateStringParameter(String paramName, String paramValue) throws IllegalArgumentException {
        if (StringUtils.isBlank(paramValue)) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }
    }

    // Validate pagination parameters
    private void validatePaginationParameters(int pageNo, int pageSize) throws IllegalArgumentException {
        if (pageNo < 1) {
            throw new IllegalArgumentException("Page number must be greater than 0.");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }
    }

    // Extract id from full key string
    private String extractIdFromKey(String key) {
        return key.substring(key.lastIndexOf('/') + 1);
    }
}
