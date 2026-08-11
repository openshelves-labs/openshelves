package com.openshelves.services.metadata.client.comicvine;

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

/// Client implementation for the Comic Vine API.
///
/// As its name suggests, this client is specifically tailored for comic books, graphic novels,
/// and manga. It is the go-to provider for fetching structured metadata about issues, volumes,
/// and character appearances that general book databases lack.
///
/// Uses Comic Vine's flat `/search/` endpoint (covering both `volume` and `issue`
/// resources) rather than replicating the volume-then-issue disambiguation some clients
/// perform — sufficient for catalog-entry metadata, which doesn't need Comic Vine's full
/// per-issue creator-credit detail.
///
/// @see ComicVineMapper
@Slf4j
@Service
@RequiredArgsConstructor
public class ComicVineClient implements MetadataClient {

    private static final String COMIC_VINE_BASE_URL = "https://comicvine.gamespot.com/api";
    private static final int DEFAULT_SEARCH_LIMIT = 5;

    private static final String VOLUME_ISSUE_FIELDS =
        "resource_type,id,name,issue_number,description,deck,image,volume," +
        "publisher,start_year,store_date,cover_date,person_credits";

    private static final String PERSON_FIELDS = "resource_type,id,name,description,deck,image";

    @Qualifier("comicVine")
    private final OkHttpClient httpClient;

    private final ComicVineMapper cvMapper;
    private final JsonMapper jsonMapper;

    /// Comic Vine API key. Free to obtain from a Comic Vine account; requests are skipped
    /// entirely when unset, since Comic Vine rejects unauthenticated calls outright.
    private String apiKey;      // TODO: Bind the API Key


    @Override
    public MetadataProvider provider() {
        return MetadataProvider.COMIC_VINE;
    }

    @Override
    public List<ExternalBook> fetchBooks(BookRequest request, FetchOptions fetchOptions) {
        // Comic Vine has no ISBN search of its own (comics rarely carry ISBNs); title is the
        // only meaningful search dimension here.
        if (StringUtils.isBlank(request.getTitle())) {
            return Collections.emptyList();
        }

        int maxResults = fetchOptions.getMaxResults() != null ? fetchOptions.getMaxResults() : DEFAULT_SEARCH_LIMIT;

        HttpUrl url = parseAndGetNewURLBuilder("/search/")
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("format", "json")
            .addQueryParameter("resources", "volume,issue")
            .addQueryParameter("query", request.getTitle())
            .addQueryParameter("limit", String.valueOf(maxResults))
            .addQueryParameter("field_list", VOLUME_ISSUE_FIELDS)
            .build();

        Optional<String> resp = performApiRequest(new Request.Builder().url(url).build());
        if (resp.isEmpty()) {
            return Collections.emptyList();
        }

        JsonNode results = extractResults(resp.get());
        if (results == null) {
            return Collections.emptyList();
        }

        List<ExternalBook> bookList = new ArrayList<>();
        for (JsonNode result : results) {
            String resourceType = text(result, "resource_type");
            if ("issue".equals(resourceType)) {
                bookList.add(cvMapper.issueToExternalBook(result));
            } else if ("volume".equals(resourceType)) {
                bookList.add(cvMapper.volumeToExternalBook(result));
            }
        }
        return bookList;
    }

    @Override
    public List<ExternalAuthor> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
        if (StringUtils.isBlank(request.getName())) {
            // Only supports finding authors by name; no other identifiers are supported by Comic Vine's public API.
            return Collections.emptyList();
        }

        int maxResults = fetchOptions.getMaxResults() != null ? fetchOptions.getMaxResults() : DEFAULT_SEARCH_LIMIT;

        HttpUrl url = parseAndGetNewURLBuilder("/search/")
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("format", "json")
            .addQueryParameter("resources", "person")
            .addQueryParameter("query", request.getName())
            .addQueryParameter("limit", String.valueOf(maxResults))
            .addQueryParameter("field_list", PERSON_FIELDS)
            .build();

        Optional<String> resp = performApiRequest(new Request.Builder().url(url).build());
        if (resp.isEmpty()) {
            return Collections.emptyList();
        }

        JsonNode results = extractResults(resp.get());
        if (results == null) {
            return Collections.emptyList();
        }

        List<ExternalAuthor> authorList = new ArrayList<>();
        for (JsonNode result : results) {
            authorList.add(cvMapper.toExternalAuthor(result));
        }
        return authorList;
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asString();
    }

    // Parses the response body and extracts the top-level "results" array.
    private JsonNode extractResults(String responseBody) {
        JsonNode root = jsonMapper.readTree(responseBody);

        int statusCode = root.has("status_code") ? root.get("status_code").asInt() : -1;
        if (statusCode != 1) {
            log.warn("Comic Vine API returned status_code={}, error={}", statusCode,
                root.has("error") ? root.get("error").asString() : "unknown");
        }

        JsonNode results = root.get("results");
        return results != null && results.isArray() ? results : null;
    }

    // Execute the HTTP request and return the response body as a String
    private Optional<String> performApiRequest(Request request) throws ThirdPartyClientException {

        if (StringUtils.isBlank(apiKey)) {
            log.debug("Comic Vine API key not configured; skipping fetch.");
            return Optional.empty();
        }

        try (Response response = httpClient.newCall(request).execute()) {
            log.debug("Comic Vine API response: {}", response);

            if (!response.isSuccessful()) {
                return handleUnsuccessfulResponse(response);
            }

            return Optional.of(response.body().string());
        } catch (IOException e) {
            throw new ThirdPartyClientException("Error occurred while making request to Comic Vine API", e);
        }
    }

    // Handle unsuccessful HTTP responses and return appropriate results or throw exceptions
    private Optional<String> handleUnsuccessfulResponse(Response response) throws ThirdPartyClientException {
        int statusCode = response.code();

        if (statusCode == 404) {
            return Optional.empty();
        }

        if (statusCode == 401 || statusCode == 403) {
            log.warn("Comic Vine API rejected the request (status={}); check the configured API key.", statusCode);
            return Optional.empty();
        }

        throw new ThirdPartyClientException("Unexpected response from Comic Vine API: " + response);
    }

    // Helper method to parse endpoint URL and return a new url builder
    private HttpUrl.Builder parseAndGetNewURLBuilder(String endpoint) throws IllegalArgumentException {
        try {
            return HttpUrl.get(COMIC_VINE_BASE_URL + endpoint).newBuilder();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Comic Vine endpoint: " + endpoint, e);
        }
    }
}
