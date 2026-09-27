package com.openshelves.metadata.client.impl.hardcover;

import com.openshelves.common.exception.ThirdPartyClientException;
import com.openshelves.metadata.client.MetadataClient;
import com.openshelves.metadata.enums.MetadataProvider;
import com.openshelves.metadata.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.*;

/// Client implementation for the Hardcover GraphQL API.
///
/// Unlike Open Library and Google Books, Hardcover exposes a single GraphQL endpoint
/// rather than REST resources, so every request here is a POST carrying a query + variables
/// payload. Requires an API token (a Hardcover account setting) injected by
/// [HardcoverInterceptor]; requests silently return no results if no token is configured,
/// since Hardcover rejects unauthenticated calls outright.
///
/// @see HardcoverMapper
@Slf4j
@Service
@RequiredArgsConstructor
public class HardcoverClient implements MetadataClient {

    private static final String HARDCOVER_GRAPHQL_URL = "https://api.hardcover.app/v1/graphql";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final int DEFAULT_SEARCH_LIMIT = 5;

    @Qualifier("hardcover")
    private final OkHttpClient httpClient;

    private final HardcoverMapper hcMapper;
    private final JsonMapper jsonMapper;

    /// Hardcover API bearer token. Requests are skipped entirely when unset, since
    /// Hardcover's GraphQL endpoint has no meaningful unauthenticated tier.
    private String apiKey;      // TODO: Bind the API Key

    private static final String EDITION_FIELDS = """
        isbn_10
        isbn_13
        language { code2 }
        publisher { name }
        """;

    private static final String BOOK_FIELDS = """
        id
        title
        subtitle
        description
        pages
        release_date
        image { url }
        cached_contributors
        """;


    @Override
    public MetadataProvider provider() {
        return MetadataProvider.HARDCOVER;
    }


    // -----------------------------------------------------------------------
    // Book Metadata Fetching
    // -----------------------------------------------------------------------

    /// Fetches book metadata from Hardcover based on the provided request parameters.
    /// Hardcover supports searching by ISBN (10 or 13) and by title. If an ISBN is provided, it is prioritized;
    /// if no ISBN is found, a title search is performed.
    @Override
    public List<BookMetadata> fetchBooks(BookRequest request, FetchOptions fetchOptions) {
        int maxResults = fetchOptions.getMaxResults() != null ? fetchOptions.getMaxResults() : DEFAULT_SEARCH_LIMIT;

        // Hardcover Supported Identifiers
        if (StringUtils.isNotBlank(request.getIsbn13()) || StringUtils.isNotBlank(request.getIsbn10())) {
            String isbn = StringUtils.isNotBlank(request.getIsbn13()) ? request.getIsbn13() : request.getIsbn10();

            String query = """
                query BooksByIsbn($isbn: String!, $limit: Int!) {
                  books(
                    where: {editions: {_or: [{isbn_13: {_eq: $isbn}}, {isbn_10: {_eq: $isbn}}]}}
                    limit: $limit
                  ) {
                    %s
                    editions(where: {_or: [{isbn_13: {_eq: $isbn}}, {isbn_10: {_eq: $isbn}}]}, limit: 1) {
                      %s
                    }
                  }
                }
                """.formatted(BOOK_FIELDS, EDITION_FIELDS);

            List<BookMetadata> bookList = executeBookQuery(query, Map.of("isbn", isbn, "limit", maxResults));
            if (!bookList.isEmpty()) return bookList;

            log.warn("Could not fetch book from Hardcover with known identifiers [isbn={}], falling back to search", isbn);
        }

        // Search For The Book using title (Hardcover has no free-text author filter at the
        // GraphQL level for this query shape, so author matching is applied client-side)
        if (StringUtils.isNotBlank(request.getTitle())) {
            String query = """
                query BooksByTitle($title: String!, $limit: Int!) {
                  books(where: {title: {_ilike: $title}}, limit: $limit) {
                    %s
                    editions(limit: 1) {
                      %s
                    }
                  }
                }
                """.formatted(BOOK_FIELDS, EDITION_FIELDS);

            List<BookMetadata> bookList = executeBookQuery(query,
                Map.of("title", "%" + request.getTitle() + "%", "limit", maxResults));

            if (request.getAuthors() != null && !request.getAuthors().isEmpty()) {
                String firstAuthor = request.getAuthors().getFirst().toLowerCase();
                bookList = bookList.stream()
                    .filter(book -> book.getAuthors().stream()
                        .anyMatch(author -> author.getName() != null && author.getName().toLowerCase().contains(firstAuthor)))
                    .toList();
            }

            if (!bookList.isEmpty()) return bookList;
        }

        // Unknown Book
        return Collections.emptyList();
    }


    // -----------------------------------------------------------------------
    // Author Metadata Fetching
    // -----------------------------------------------------------------------

    /// Fetches author metadata from Hardcover based on the provided request parameters.
    /// Hardcover only supports searching by author name; other identifiers (e.g., VIAF, ISNI) are not supported by the API.
    @Override
    public List<AuthorMetadata> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
        if (StringUtils.isBlank(request.getName())) {
            // Only supports finding authors by name; no other identifiers are supported by the Hardcover API.
            return Collections.emptyList();
        }

        int maxResults = fetchOptions.getMaxResults() != null ? fetchOptions.getMaxResults() : DEFAULT_SEARCH_LIMIT;

        String query = """
            query AuthorsByName($name: String!, $limit: Int!) {
              authors(where: {name: {_ilike: $name}}, limit: $limit) {
                id
                name
                bio
                born_date
                death_date
                image { url }
              }
            }
            """;

        Optional<String> resp = performGraphQLRequest(query, Map.of("name", "%" + request.getName() + "%", "limit", maxResults));
        if (resp.isEmpty()) {
            return Collections.emptyList();
        }

        JsonNode authors = extractDataArray(resp.get(), "authors");
        if (authors == null) {
            return Collections.emptyList();
        }

        List<AuthorMetadata> authorList = new ArrayList<>();
        for (JsonNode authorNode : authors) {
            authorList.add(hcMapper.convertToAuthorMetadata(authorNode));
        }

        return authorList;
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    /// Runs a GraphQL query for books and maps its results to metadata.
    ///
    /// @param query the GraphQL query
    /// @param variables the values supplied to the query
    /// @return the mapped books, or an empty list if the request has no result array
    private List<BookMetadata> executeBookQuery(String query, Map<String, Object> variables) {
        Optional<String> resp = performGraphQLRequest(query, variables);
        if (resp.isEmpty()) {
            return Collections.emptyList();
        }

        JsonNode books = extractDataArray(resp.get(), "books");
        if (books == null) {
            return Collections.emptyList();
        }

        List<BookMetadata> bookList = new ArrayList<>();
        for (JsonNode bookNode : books) {
            bookList.add(hcMapper.convertToBookMetadata(bookNode));
        }

        return bookList;
    }

    /// Extracts a named array from the `data` object in a GraphQL response.
    ///
    /// GraphQL-level errors are logged; a missing or non-array field returns `null`.
    ///
    /// @param responseBody the GraphQL response body
    /// @param field the name of the array field in `data`
    /// @return the requested array, or `null` if it is unavailable
    private JsonNode extractDataArray(String responseBody, String field) {
        JsonNode root = jsonMapper.readTree(responseBody);

        if (root.has("errors") && !root.get("errors").isNull()) {
            log.warn("Hardcover GraphQL API returned errors: {}", root.get("errors"));
        }

        JsonNode data = root.get("data");
        if (data == null || data.isNull() || !data.has(field)) {
            return null;
        }

        JsonNode array = data.get(field);
        return array.isArray() ? array : null;
    }

    /// Executes a GraphQL request and returns its successful response body.
    ///
    /// If no API key is configured or the API returns `401`, `403`, or `404`, an
    /// empty result is returned.
    ///
    /// @param query the GraphQL query
    /// @param variables the values supplied to the query
    /// @return the response body, or an empty value when the request is skipped or rejected
    /// @throws ThirdPartyClientException if the request fails or the API returns an unexpected status
    private Optional<String> performGraphQLRequest(String query, Map<String, Object> variables) throws ThirdPartyClientException {

        if (StringUtils.isBlank(apiKey)) {
            log.debug("Hardcover API key not configured; skipping fetch.");
            return Optional.empty();
        }

        ObjectNode body = jsonMapper.createObjectNode();
        body.put("query", query);
        body.set("variables", jsonMapper.valueToTree(variables));

        RequestBody requestBody = RequestBody.create(body.toString(), JSON);
        Request request = new Request.Builder()
            .url(HARDCOVER_GRAPHQL_URL)
            .post(requestBody)
            .addHeader("Authorization", "Bearer " + apiKey)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            log.debug("Hardcover API response: {}", response);

            if (!response.isSuccessful()) {
                return handleUnsuccessfulResponse(response);
            }

            return Optional.of(response.body().string());
        } catch (IOException e) {
            throw new ThirdPartyClientException("Error occurred while making request to Hardcover API", e);
        }
    }

    /// Handles unsuccessful HTTP responses and returns appropriate results or throws exceptions.
    ///
    /// Authentication failures and `404` responses return an empty result; other
    /// unsuccessful statuses are treated as unexpected responses.
    ///
    /// @param response the unsuccessful HTTP response
    /// @return an empty result for `401`, `403`, or `404`
    /// @throws ThirdPartyClientException for any other unsuccessful status
    private Optional<String> handleUnsuccessfulResponse(Response response) throws ThirdPartyClientException {
        int statusCode = response.code();

        if (statusCode == 401 || statusCode == 403) {
            log.warn("Hardcover API rejected the request (status={}); check the configured API key.", statusCode);
            return Optional.empty();
        }

        if (statusCode == 404) {
            return Optional.empty();
        }

        throw new ThirdPartyClientException("Unexpected response from Hardcover API: " + response);
    }
}
