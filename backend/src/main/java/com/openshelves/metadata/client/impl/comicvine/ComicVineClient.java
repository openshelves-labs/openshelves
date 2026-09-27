package com.openshelves.metadata.client.impl.comicvine;

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


    // -----------------------------------------------------------------------
    // Book Metadata Fetching
    // -----------------------------------------------------------------------

    /// Fetches book metadata from Comic Vine based on the provided request parameters.
    /// Comic Vine only supports searching by book title, as comics rarely carry ISBNs.
    @Override
    public List<BookMetadata> fetchBooks(BookRequest request, FetchOptions fetchOptions) {
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

        List<BookMetadata> bookList = new ArrayList<>();

        for (JsonNode result : results) {
            String resourceType = text(result, "resource_type");
            if ("issue".equals(resourceType)) {
                bookList.add(cvMapper.convertIssueToBookMetadata(result));
            } else if ("volume".equals(resourceType)) {
                bookList.add(cvMapper.convertVolumeToBookMetadata(result));
            }
        }

        return bookList;
    }


    // -----------------------------------------------------------------------
    // Author Metadata Fetching
    // -----------------------------------------------------------------------

    /// Fetches author metadata from Comic Vine based on the provided request parameters.
    /// Comic Vine only supports searching by author name; other identifiers (e.g., VIAF, ISNI) are not supported by the API.
    @Override
    public List<AuthorMetadata> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
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

        List<AuthorMetadata> authorList = new ArrayList<>();
        for (JsonNode result : results) {
            authorList.add(cvMapper.convertToAuthorMetadata(result));
        }

        return authorList;
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    /// Safely extracts a text field from a JSON node, returning `null` if the node,
    /// field, or field value is `null`.
    ///
    /// @param node the JSON node to read; may be `null`
    /// @param field the name of the field to extract
    /// @return the field's text value, or `null` if unavailable
    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asString();
    }

    /// Extracts the `results` array from a Comic Vine response.
    ///
    /// Logs a warning if the API's `status_code` indicates an unsuccessful result.
    ///
    /// @param responseBody the API response body
    /// @return the results array, or `null` if the response does not contain one
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

    /// Executes a Comic Vine request and returns its successful response body.
    ///
    /// If no API key is configured, the request is skipped.
    ///
    /// @param request the request to execute
    /// @return the response body, or an empty value if the request is skipped or the resource is not found
    /// @throws ThirdPartyClientException if the request fails or the API returns an unexpected status
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

    /// Handles unsuccessful HTTP responses and returns appropriate results or throws exceptions.
    ///
    /// Responses with status `401`, `403`, or `404` return an empty result; other
    /// unsuccessful statuses are treated as unexpected responses.
    ///
    /// @param response the unsuccessful HTTP response
    /// @return an empty result for `401`, `403`, or `404`
    /// @throws ThirdPartyClientException for any other unsuccessful status
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

    /// Creates a URL builder for a Comic Vine endpoint.
    ///
    /// @param endpoint the endpoint path to append to the Comic Vine base URL
    /// @return the builder for the resulting URL
    /// @throws IllegalArgumentException if the endpoint does not form a valid URL
    private HttpUrl.Builder parseAndGetNewURLBuilder(String endpoint) throws IllegalArgumentException {
        try {
            return HttpUrl.get(COMIC_VINE_BASE_URL + endpoint).newBuilder();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Comic Vine endpoint: " + endpoint, e);
        }
    }
}
