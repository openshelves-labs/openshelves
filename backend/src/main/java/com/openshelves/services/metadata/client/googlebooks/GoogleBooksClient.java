package com.openshelves.services.metadata.client.googlebooks;

import com.openshelves.exception.ThirdPartyClientException;
import com.openshelves.model.dto.metadata.*;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.services.metadata.MetadataClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.*;

/// Client implementation for the Google Books API.
///
/// Communicates with the Google Books "volumes" endpoint to search for book metadata,
/// mapping responses via [GoogleBooksMapper]. Unlike Open Library, Google Books has no
/// dedicated author-lookup endpoint, so author search is implemented by scanning volume
/// results for matching contributor names — see [#fetchAuthors] for details.
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleBooksClient implements MetadataClient {

    private static final String GOOGLE_BOOKS_BASE_URL = "https://www.googleapis.com/books/v1";
    private static final int DEFAULT_SEARCH_LIMIT = 5;

    @Qualifier("googleBooks")
    private final OkHttpClient httpClient;

    private final GoogleBooksMapper gbMapper;
    private final JsonMapper jsonMapper;

    /// Google Books API key. Required for authentication with the Google Books API.
    private String apiKey;      // TODO: Bind the API Key


    @Override
    public MetadataProvider provider() {
        return MetadataProvider.GOOGLE_BOOKS;
    }

    @Override
    public List<ExternalBook> fetchBooks(BookRequest request, FetchOptions fetchOptions) {
        // Google Books Supported Identifiers: ISBN-10 and ISBN-13 only (no ASIN/OLID)
        if (StringUtils.isNotBlank(request.getIsbn13()) || StringUtils.isNotBlank(request.getIsbn10())) {
            String isbn = StringUtils.isNotBlank(request.getIsbn13()) ? request.getIsbn13() : request.getIsbn10();

            HttpUrl url = buildVolumesUrl("isbn:" + isbn, fetchOptions, 1).build();
            Request req = new Request.Builder().url(url).build();

            Optional<String> resp = performApiRequest(req);
            if (resp.isPresent()) {
                List<ExternalBook> bookList = extractBooks(resp.get());
                if (!bookList.isEmpty()) return bookList;
            }

            log.warn("Could not fetch book from Google Books with known identifiers [isbn={}], falling back to search", isbn);
        }

        // Search For The Book using title and author
        if (StringUtils.isNotBlank(request.getTitle())) {
            String query = "intitle:" + quoteIfMultiWord(request.getTitle());

            if (request.getAuthors() != null && !request.getAuthors().isEmpty()) {
                // Use only the first author; combining multiple authors risks over-filtering
                query += "+inauthor:" + quoteIfMultiWord(request.getAuthors().getFirst());
            }

            int maxResults = fetchOptions.getMaxResults() != null ? fetchOptions.getMaxResults() : DEFAULT_SEARCH_LIMIT;
            HttpUrl url = buildVolumesUrl(query, fetchOptions, maxResults).build();
            Request req = new Request.Builder().url(url).build();

            Optional<String> resp = performApiRequest(req);
            if (resp.isPresent()) {
                List<ExternalBook> bookList = extractBooks(resp.get());
                if (!bookList.isEmpty()) return bookList;
            }
        }

        // Unknown Book
        return Collections.emptyList();
    }

    @Override
    public List<ExternalAuthor> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
        // Google Books has no author-lookup endpoint — the "authors" array on a volume is
        // just free-text names attached to that edition, with no bio, birth year, or stable
        // identity behind it. Scanning volume search results for name matches (as this used
        // to do) produces ExternalAuthors with nothing but a name, which is worse than no
        // data: it can silently win a field-resolution vote over a provider with real author
        // detail. Disabled until Google Books exposes something with actual author fields.
        return Collections.emptyList();
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    // Parses the volumes.list response body and maps each item's volumeInfo to an ExternalBook.
    private List<ExternalBook> extractBooks(String responseBody) {
        JsonNode root = jsonMapper.readTree(responseBody);
        JsonNode items = root.get("items");
        if (items == null || !items.isArray()) {
            return Collections.emptyList();
        }

        List<ExternalBook> bookList = new ArrayList<>();
        for (JsonNode item : items) {
            JsonNode volumeInfo = item.get("volumeInfo");
            if (volumeInfo != null) {
                bookList.add(gbMapper.toExternalBook(volumeInfo));
            }
        }
        return bookList;
    }

    // Wraps a multi-word search term in quotes so Google Books treats it as a phrase
    // rather than matching each word independently.
    private String quoteIfMultiWord(String term) {
        String trimmed = term.trim();
        return trimmed.contains(" ") ? "\"" + trimmed + "\"" : trimmed;
    }

    // Builds the base /volumes search URL with query, language restriction, result limit,
    // and API key applied uniformly across all search paths.
    private HttpUrl.Builder buildVolumesUrl(String query, FetchOptions fetchOptions, int maxResults) {
        HttpUrl.Builder urlBuilder = parseAndGetNewURLBuilder("/volumes");

        urlBuilder.addQueryParameter("q", query);
        urlBuilder.addQueryParameter("maxResults", String.valueOf(Math.min(maxResults, 40)));

        if (StringUtils.isNotBlank(fetchOptions.getLanguage())) {
            urlBuilder.addQueryParameter("langRestrict", fetchOptions.getLanguage());
        }

        if (StringUtils.isNotBlank(apiKey)) {
            urlBuilder.addQueryParameter("key", apiKey);
        }

        return urlBuilder;
    }

    // Execute the HTTP request and return the response body as a String
    private Optional<String> performApiRequest(Request request) throws ThirdPartyClientException {

        if (StringUtils.isBlank(apiKey)) {
            log.debug("Google Books API key not configured; skipping fetch.");
            return Optional.empty();
        }

        try (Response response = httpClient.newCall(request).execute()) {
            log.debug("Google Books API response: {}", response);

            // Handle unexpected HTTP status codes
            if (!response.isSuccessful()) {
                return handleUnsuccessfulResponse(response);
            }

            // Return the response body as a String
            return Optional.of(response.body().string());
        } catch (IOException e) {
            throw new ThirdPartyClientException("Error occurred while making request to Google Books API", e);
        }
    }

    // Handle unsuccessful HTTP responses and return appropriate results or throw exceptions
    private Optional<String> handleUnsuccessfulResponse(Response response) throws ThirdPartyClientException {
        int statusCode = response.code();

        if (statusCode == 404) {       // No results found
            return Optional.empty();
        }

        throw new ThirdPartyClientException("Unexpected response from Google Books API: " + response);
    }

    // Helper method to parse endpoint URL and return a new url builder
    private HttpUrl.Builder parseAndGetNewURLBuilder(String endpoint) throws IllegalArgumentException {
        try {
            return HttpUrl.get(GOOGLE_BOOKS_BASE_URL + endpoint).newBuilder();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Google Books endpoint: " + endpoint, e);
        }
    }
}
