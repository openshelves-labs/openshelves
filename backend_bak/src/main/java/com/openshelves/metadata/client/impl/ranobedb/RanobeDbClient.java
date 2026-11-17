package com.openshelves.metadata.client.impl.ranobedb;

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

/// Client implementation for the RanobeDB API.
///
/// This client acts as the primary metadata source for Light Novels (or "Ranobe" in Japanese).
/// It's particularly useful for fetching specialized metadata for Japanese literature that
/// general-purpose providers might miss.
///
/// RanobeDB requires no API key — it's a small, open, community-run project — but its
/// `/books` search endpoint only returns bare IDs, so a full result requires a second
/// `/book/{id}` fetch per hit.
///
/// @see RanobeDbMapper
@Slf4j
@Service
@RequiredArgsConstructor
public class RanobeDbClient implements MetadataClient {

    private static final String RANOBEDB_BASE_URL = "https://ranobedb.org/api/v0";
    private static final int DEFAULT_SEARCH_LIMIT = 5;

    @Qualifier("ranobeDb")
    private final OkHttpClient httpClient;

    private final RanobeDbMapper rdbMapper;
    private final JsonMapper jsonMapper;

    @Override
    public MetadataProvider provider() {
        return MetadataProvider.RANOBEDB;
    }


    // -----------------------------------------------------------------------
    // Book Metadata Fetching
    // -----------------------------------------------------------------------

    /// Fetches book metadata from RanobeDB based on the provided request parameters.
    /// If the request lacks a title, an empty list is returned, as RanobeDB's public search
    /// does not support ISBN-specific queries.
    @Override
    public List<BookMetadata> fetchBooks(BookRequest request, FetchOptions fetchOptions) {
        // RanobeDB's public search has no ISBN-specific filter; title is the only
        // meaningful search dimension available here.
        if (StringUtils.isBlank(request.getTitle())) {
            return Collections.emptyList();
        }

        int maxResults = fetchOptions.getMaxResults() != null ? fetchOptions.getMaxResults() : DEFAULT_SEARCH_LIMIT;

        HttpUrl searchUrl = parseAndGetNewURLBuilder("/books")
            .addQueryParameter("q", request.getTitle())
            .addQueryParameter("query", request.getTitle())
            .addQueryParameter("limit", String.valueOf(maxResults))
            .addQueryParameter("rl", "en")
            .addQueryParameter("rll", "or")
            .addQueryParameter("rf", "digital,print")
            .addQueryParameter("rfl", "or")
            .build();

        Optional<String> searchResp = performApiRequest(new Request.Builder().url(searchUrl).build());
        if (searchResp.isEmpty()) {
            return Collections.emptyList();
        }

        JsonNode books = jsonMapper.readTree(searchResp.get()).get("books");
        if (books == null || !books.isArray()) {
            return Collections.emptyList();
        }

        List<BookMetadata> bookList = new ArrayList<>();
        for (JsonNode hit : books) {
            JsonNode idNode = hit.get("id");
            if (idNode == null || idNode.isNull()) continue;

            fetchBookDetails(idNode.asString()).ifPresent(bookList::add);
        }

        return bookList;
    }


    // -----------------------------------------------------------------------
    // Author Metadata Fetching
    // -----------------------------------------------------------------------

    /// Author metadata fetching is not supported by RanobeDB's public API, so this method returns an empty list.
    @Override
    public List<AuthorMetadata> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
        // RanobeDB has no dedicated author-lookup endpoint in its public API.
        return Collections.emptyList();
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    /// Fetches a book's details from RanobeDB and maps them to metadata.
    ///
    /// @param bookId the RanobeDB identifier of the book
    /// @return the mapped book, or an empty value if the response contains no book
    private Optional<BookMetadata> fetchBookDetails(String bookId) {
        HttpUrl url = parseAndGetNewURLBuilder("/book/" + bookId).build();
        Request req = new Request.Builder().url(url).build();

        Optional<String> resp = performApiRequest(req);
        if (resp.isEmpty()) {
            return Optional.empty();
        }

        JsonNode book = jsonMapper.readTree(resp.get()).get("book");
        if (book == null || book.isNull()) {
            return Optional.empty();
        }

        return Optional.of(rdbMapper.convertToBookMetadata(book));
    }

    /// Executes a RanobeDB request and returns its successful response body.
    ///
    /// @param request the request to execute
    /// @return the response body, or an empty value if the resource is not found
    /// @throws ThirdPartyClientException if the request fails or the API returns an unexpected status
    private Optional<String> performApiRequest(Request request) throws ThirdPartyClientException {
        try (Response response = httpClient.newCall(request).execute()) {
            log.debug("RanobeDB API response: {}", response);

            if (!response.isSuccessful()) {
                return handleUnsuccessfulResponse(response);
            }

            return Optional.of(response.body().string());
        } catch (IOException e) {
            throw new ThirdPartyClientException("Error occurred while making request to RanobeDB API", e);
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

        if (statusCode == 404) {
            return Optional.empty();
        }

        throw new ThirdPartyClientException("Unexpected response from RanobeDB API: " + response);
    }

    /// Creates a URL builder for a RanobeDB endpoint.
    ///
    /// @param endpoint the endpoint path to append to the RanobeDB base URL
    /// @return the builder for the resulting URL
    /// @throws IllegalArgumentException if the endpoint does not form a valid URL
    private HttpUrl.Builder parseAndGetNewURLBuilder(String endpoint) throws IllegalArgumentException {
        try {
            return HttpUrl.get(RANOBEDB_BASE_URL + endpoint).newBuilder();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid RanobeDB endpoint: " + endpoint, e);
        }
    }
}
