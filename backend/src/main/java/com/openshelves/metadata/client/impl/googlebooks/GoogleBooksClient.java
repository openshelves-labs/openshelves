package com.openshelves.metadata.client.impl.googlebooks;

import com.openshelves.common.exception.ThirdPartyClientException;
import com.openshelves.metadata.client.MetadataClient;
import com.openshelves.metadata.enums.MetadataProvider;
import com.openshelves.metadata.model.*;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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


    // -----------------------------------------------------------------------
    // Book Metadata Fetching
    // -----------------------------------------------------------------------

    /// Fetches book metadata from Google Books based on the provided [BookRequest] and [FetchOptions].
    /// The method first attempts to fetch the book using known identifiers (ISBN-10 or ISBN-13).
    /// If no results are found, it falls back to searching by title and author.
    @Override
    public List<BookMetadata> fetchBooks(BookRequest request, FetchOptions fetchOptions) {

        // Google Books Supported Identifiers: ISBN-10 and ISBN-13 only (no ASIN/OLID)
        if (StringUtils.isNotBlank(request.getIsbn13()) || StringUtils.isNotBlank(request.getIsbn10())) {
            String isbn = StringUtils.isNotBlank(request.getIsbn13()) ? request.getIsbn13() : request.getIsbn10();

            HttpUrl url = buildVolumesUrl("isbn:" + isbn, fetchOptions, 1).build();
            Request req = new Request.Builder().url(url).build();

            Optional<String> resp = performApiRequest(req);
            if (resp.isPresent()) {
                List<BookMetadata> bookList = extractBookMetadataFromResponse(resp.get());
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
                List<BookMetadata> bookList = extractBookMetadataFromResponse(resp.get());
                if (!bookList.isEmpty()) return bookList;
            }
        }

        // Unknown Book
        return Collections.emptyList();
    }


    // -----------------------------------------------------------------------
    // Author Metadata Fetching
    // -----------------------------------------------------------------------

    /// Author search is not supported by Google Books, as the API does not provide a dedicated author-lookup endpoint.
    @Override
    public List<AuthorMetadata> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
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

    /// Extracts book metadata from the `items` array in a Google Books volumes response.
    ///
    /// Each item's `volumeInfo` is mapped to [BookMetadata].
    ///
    /// @param responseBody the JSON response body from the volumes endpoint
    /// @return the mapped books, or an empty list if the response has no `items` array
    private List<BookMetadata> extractBookMetadataFromResponse(String responseBody) {
        JsonNode root = jsonMapper.readTree(responseBody);
        JsonNode items = root.get("items");
        if (items == null || !items.isArray()) {
            return Collections.emptyList();
        }

        List<BookMetadata> bookList = new ArrayList<>();
        for (JsonNode item : items) {
            JsonNode volumeInfo = item.get("volumeInfo");
            if (volumeInfo != null) {
                bookList.add(gbMapper.convertToBookMetadata(volumeInfo));
            }
        }
        return bookList;
    }

    /// Wraps a multi-word search term in quotes so Google Books treats it as a phrase
    /// rather than matching each word independently.
    ///
    /// @param term the search term
    /// @return the trimmed term, quoted when it contains multiple words
    private String quoteIfMultiWord(String term) {
        String trimmed = term.trim();
        return trimmed.contains(" ") ? "\"" + trimmed + "\"" : trimmed;
    }

    /// Builds the base `/volumes` search URL with the query, language restriction,
    /// result limit, and API key applied uniformly across all search paths.
    ///
    /// @param query the Google Books query expression
    /// @param fetchOptions language restriction options
    /// @param maxResults the maximum number of requested results
    /// @return the URL builder for the volumes search
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

    /// Executes a Google Books request and returns the successful response body.
    ///
    /// If no API key is configured, the request is skipped. A `404` response also
    /// produces an empty result; other unsuccessful responses and request failures
    /// result in a [ThirdPartyClientException].
    ///
    /// @param request the request to execute
    /// @return the response body, or an empty value when fetching is skipped or the resource is not found
    /// @throws ThirdPartyClientException if the request fails or the API returns an unexpected status
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

    /// Handles unsuccessful HTTP responses and returns appropriate results or throws exceptions.
    ///
    /// A `404` indicates that no matching result was found; other statuses are
    /// treated as unexpected responses.
    ///
    /// @param response the unsuccessful HTTP response
    /// @return an empty result when the response status is `404`
    /// @throws ThirdPartyClientException if the response has any other unsuccessful status
    private Optional<String> handleUnsuccessfulResponse(Response response) throws ThirdPartyClientException {
        int statusCode = response.code();

        if (statusCode == 404) {       // No results found
            return Optional.empty();
        }

        throw new ThirdPartyClientException("Unexpected response from Google Books API: " + response);
    }

    /// Creates a URL builder for a Google Books endpoint.
    ///
    /// @param endpoint the endpoint path to append to the Google Books base URL
    /// @return the builder for the resulting URL
    /// @throws IllegalArgumentException if the endpoint does not form a valid URL
    private HttpUrl.Builder parseAndGetNewURLBuilder(String endpoint) throws IllegalArgumentException {
        try {
            return HttpUrl.get(GOOGLE_BOOKS_BASE_URL + endpoint).newBuilder();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Google Books endpoint: " + endpoint, e);
        }
    }
}
