package com.openshelves.services.metadata.client.openlibrary;

import com.jayway.jsonpath.JsonPath;
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

/// Client implementation for the Open Library API.
///
/// Communicates with Open Library to search for and retrieve book and author
/// metadata, mapping the responses via [OpenLibraryMapper].
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenLibraryClient implements MetadataClient {

    private static final String OPEN_LIBRARY_BASE_URL = "https://openlibrary.org";
    private static final int DEFAULT_SEARCH_LIMIT = 5;

    @Qualifier("openLibrary")
    private final OkHttpClient httpClient;

    private final OpenLibraryMapper olMapper;
    private final JsonMapper jsonMapper;


    @Override
    public MetadataProvider provider() {
        return MetadataProvider.OPEN_LIBRARY;
    }

    @Override
    public List<ExternalBook> fetchBooks(BookRequest request, FetchOptions fetchOptions) {
        // OpenLibrary Supported Identifiers
        if (StringUtils.isNotBlank(request.getIsbn13())
            || StringUtils.isNotBlank(request.getIsbn10())
            || StringUtils.isNotBlank(request.getOlid())) {

            HttpUrl url = buildBookUrl(request).build();
            Request req = new Request.Builder().url(url).build();

            Optional<String> resp = performApiRequest(req);
            if (resp.isPresent()) {
                JsonNode editionNode = jsonMapper.readTree(resp.get());
                JsonNode workNode = olMapper.needsWorkEnrichment(editionNode) ?
                    fetchWorkNode(editionNode) : null;
                return Collections.singletonList(olMapper.toExternalBook(editionNode, workNode));
            }

            log.warn("Could not fetch book from Open Library with known identifiers [path={}], falling back to search", url.encodedPath());
        }

        // Search For The Book using title and author
        if (StringUtils.isNotBlank(request.getTitle())) {
            HttpUrl url = buildBookSearchUrl(request, fetchOptions).build();
            Request req = new Request.Builder().url(url).build();

            Optional<String> resp = performApiRequest(req);
            if (resp.isPresent()) {
                List<String> editionKeyList = JsonPath.read(resp.get(), "$.docs[*].editions.docs[*].key");

                List<ExternalBook> bookList = new ArrayList<>();
                for (String editionKey : editionKeyList) {
                    // Fetch individual book details using the edition key

                    // Note: Here edition key already contains the prefix "/books/", so we can directly append ".json" to it
                    HttpUrl bookUrl = parseAndGetNewURLBuilder(editionKey + ".json").build();
                    Request bookReq = new Request.Builder().url(bookUrl).build();

                    Optional<String> bookResp = performApiRequest(bookReq);
                    if (bookResp.isPresent()) {
                        JsonNode editionNode = jsonMapper.readTree(bookResp.get());
                        JsonNode workNode = olMapper.needsWorkEnrichment(editionNode) ?
                            fetchWorkNode(editionNode) : null;
                        bookList.add(olMapper.toExternalBook(editionNode, workNode));
                    }
                }

                if (!bookList.isEmpty()) return bookList;
            }
        }

        // Unknown Book
        return Collections.emptyList();
    }

    /// Extracts the work key from an edition's JSON node and fetches the corresponding
    /// work details from the Open Library API.
    ///
    /// @param editionNode the JSON node representing the edition data
    /// @return the JSON node representing the work data, or {@code null} if it cannot be found or fetched
    private JsonNode fetchWorkNode(JsonNode editionNode) {
        // Retrieve the "works" array from the edition node
        JsonNode works = editionNode.get("works");
        if (works == null || !works.isArray() || works.isEmpty()) {
            log.warn("Edition node does not contain 'works' array or it is empty: {}", editionNode);
            return null;
        }

        // Get the key of the first work in the array
        JsonNode keyNode = works.get(0).get("key");
        if (keyNode == null || !keyNode.isString()) return null;

        // Construct the URL to fetch the work data
        HttpUrl workUrl = parseAndGetNewURLBuilder(keyNode.asString() + ".json").build();
        Request workReq = new Request.Builder().url(workUrl).build();

        // Perform the API request and parse the response
        Optional<String> workResp = performApiRequest(workReq);
        return workResp.map(jsonMapper::readTree).orElse(null);
    }

    // Returns the URL to fetch book data based on the request parameters
    private HttpUrl.Builder buildBookUrl(BookRequest request) {
        if (StringUtils.isNotBlank(request.getIsbn13())) {
            return parseAndGetNewURLBuilder("/isbn/" + request.getIsbn13() + ".json");
        } else if (StringUtils.isNotBlank(request.getIsbn10())) {
            return parseAndGetNewURLBuilder("/isbn/" + request.getIsbn10() + ".json");
        } else if (StringUtils.isNotBlank(request.getOlid())) {
            return parseAndGetNewURLBuilder("/books/" + request.getOlid() + ".json");
        } else {
            throw new IllegalArgumentException("At least one identifier (ISBN-13, ISBN-10, or OLID) must be provided.");
        }
    }

    // Returns the URL to search for books based on title and author parameters
    private HttpUrl.Builder buildBookSearchUrl(BookRequest request, FetchOptions fetchOptions) {
        HttpUrl.Builder urlBuilder = parseAndGetNewURLBuilder("/search.json");

        // Build solr query string
        String query = request.getTitle();

        if (request.getAuthors() != null && !request.getAuthors().isEmpty()) {
            // Use only the first author; multiple authors risk over-filtering on Open Library's search API
            query += " author:\"" + request.getAuthors().getFirst() + "\"";
        }

        urlBuilder.addQueryParameter("q", query);

        // Only fetch edition keys in search results to minimize payload size
        urlBuilder.addQueryParameter("fields", "editions,key");

        // Suggest Book Language
        if (StringUtils.isNotBlank(fetchOptions.getLanguage())) {
            urlBuilder.addQueryParameter("lang", fetchOptions.getLanguage());
        }

        // Apply Limits
        int maxResults = fetchOptions.getMaxResults() != null ? fetchOptions.getMaxResults() : DEFAULT_SEARCH_LIMIT;
        urlBuilder.addQueryParameter("limit", String.valueOf(maxResults));

        return urlBuilder;
    }

    @Override
    public List<ExternalAuthor> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
        // OpenLibrary Supported Identifiers
        if (StringUtils.isNotBlank(request.getOlid())) {

            HttpUrl url = buildAuthorUrl(request).build();
            Request req = new Request.Builder().url(url).build();

            Optional<String> resp = performApiRequest(req);
            if (resp.isPresent()) {
                JsonNode node = jsonMapper.readTree(resp.get());
                return Collections.singletonList(olMapper.toExternalAuthor(node));
            }

            log.warn("Could not fetch author from Open Library with known identifiers [path={}], falling back to search", url.encodedPath());
        }

        // Search For The Author using his name
        if (StringUtils.isNotBlank(request.getName())) {
            HttpUrl url = buildAuthorSearchUrl(request, fetchOptions).build();
            Request req = new Request.Builder().url(url).build();

            Optional<String> resp = performApiRequest(req);
            if (resp.isPresent()) {
                List<String> authorKeyList = JsonPath.read(resp.get(), "$.docs[*].key");

                List<ExternalAuthor> authorList = new ArrayList<>();
                for (String authorKey : authorKeyList) {
                    // Fetch individual author details using the author key
                    HttpUrl authorUrl = parseAndGetNewURLBuilder("/authors/" + authorKey + ".json").build();
                    Request authorReq = new Request.Builder().url(authorUrl).build();

                    Optional<String> authorResp = performApiRequest(authorReq);
                    if (authorResp.isPresent()) {
                        JsonNode node = jsonMapper.readTree(authorResp.get());
                        authorList.add(olMapper.toExternalAuthor(node));
                    }
                }

                if (!authorList.isEmpty()) return authorList;
            }
        }

        // Unknown Author
        return Collections.emptyList();
    }

    // Returns the URL to fetch author data based on the request parameters
    private HttpUrl.Builder buildAuthorUrl(AuthorRequest request) {
        if (StringUtils.isNotBlank(request.getOlid())) {
            return parseAndGetNewURLBuilder("/authors/" + request.getOlid() + ".json");
        } else {
            throw new IllegalArgumentException("Author OLID must be provided.");
        }
    }

    // Returns the URL to search for authors based on author name
    private HttpUrl.Builder buildAuthorSearchUrl(AuthorRequest request, FetchOptions fetchOptions) {
        HttpUrl.Builder urlBuilder = parseAndGetNewURLBuilder("/search/authors.json");

        // Build solr query string
        urlBuilder.addQueryParameter("q", request.getName());

        // Only fetch keys in search results to minimize payload size
        urlBuilder.addQueryParameter("fields", "key");

        // Apply Limits
        int maxResults = fetchOptions.getMaxResults() != null ? fetchOptions.getMaxResults() : DEFAULT_SEARCH_LIMIT;
        urlBuilder.addQueryParameter("limit", String.valueOf(maxResults));

        return urlBuilder;
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

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
}
