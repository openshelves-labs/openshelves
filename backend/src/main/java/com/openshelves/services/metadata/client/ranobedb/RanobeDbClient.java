package com.openshelves.services.metadata.client.ranobedb;

import com.openshelves.exception.ThirdPartyClientException;
import com.openshelves.model.dto.metadata.*;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.services.metadata.MetadataClient;
import com.openshelves.services.metadata.client.openlibrary.OpenLibraryClient;
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

/// Client implementation for the RanobeDB API.
///
/// This client acts as the primary metadata source for Light Novels (or "Ranobe" in Japanese).
/// It's particularly useful for fetching specialized metadata for Japanese literature that
/// general-purpose providers might miss.
///
/// RanobeDB requires no API key — it's a small, open, community-run project — but its
/// `/books` search endpoint only returns bare IDs, so a full result requires a second
/// `/book/{id}` fetch per hit, mirroring [OpenLibraryClient]'s
/// search-then-detail pattern.
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

    @Override
    public List<ExternalBook> fetchBooks(BookRequest request, FetchOptions fetchOptions) {
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

        List<ExternalBook> bookList = new ArrayList<>();
        for (JsonNode hit : books) {
            JsonNode idNode = hit.get("id");
            if (idNode == null || idNode.isNull()) continue;

            fetchBookDetail(idNode.asString()).ifPresent(bookList::add);
        }
        return bookList;
    }

    @Override
    public List<ExternalAuthor> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
        // RanobeDB has no dedicated author-lookup endpoint in its public API.
        return Collections.emptyList();
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    // Fetches full book details for a single RanobeDB book id and maps them to an ExternalBook.
    private Optional<ExternalBook> fetchBookDetail(String bookId) {
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

        return Optional.of(rdbMapper.toExternalBook(book));
    }

    // Execute the HTTP request and return the response body as a String
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

    // Handle unsuccessful HTTP responses and return appropriate results or throw exceptions
    private Optional<String> handleUnsuccessfulResponse(Response response) throws ThirdPartyClientException {
        int statusCode = response.code();

        if (statusCode == 404) {
            return Optional.empty();
        }

        throw new ThirdPartyClientException("Unexpected response from RanobeDB API: " + response);
    }

    // Helper method to parse endpoint URL and return a new url builder
    private HttpUrl.Builder parseAndGetNewURLBuilder(String endpoint) throws IllegalArgumentException {
        try {
            return HttpUrl.get(RANOBEDB_BASE_URL + endpoint).newBuilder();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid RanobeDB endpoint: " + endpoint, e);
        }
    }
}
