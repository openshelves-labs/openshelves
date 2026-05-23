package com.openshelves.model.dto.metadata;

import lombok.Builder;
import lombok.Value;

/**
 * Controls the behaviour of a metadata fetch operation.
 *
 * <p>Passed alongside a request object (e.g., {@link BookRequest} or {@link AuthorRequest})
 * to tune how results are retrieved — for example, limiting the number of results or
 * filtering by language.</p>
 *
 * <p>Instances are immutable and constructed via the builder:
 * <pre>{@code
 * FetchOptions options = FetchOptions.builder()
 *     .withLanguage("fr")
 *     .withMaxResults(3)
 *     .build();
 * }</pre>
 * </p>
 */
@Value
@Builder(setterPrefix = "with")
public class FetchOptions {

    // -------------------------------------------------------------------------
    // Filtering
    // -------------------------------------------------------------------------

    /**
     * ISO-639-1 language code to filter results by (e.g., {@code "en"}, {@code "fr"}).
     *
     * <p>When {@code null}, no language filter is applied.</p>
     */
    String language;


    // -------------------------------------------------------------------------
    // Pagination
    // -------------------------------------------------------------------------

    /**
     * Maximum number of results to return per fetch.
     *
     * <p>When {@code null}, each provider applies its own default cap.
     * For most metadata lookups a small number (e.g., {@code 5}) is sufficient —
     * callers rarely need a long list when resolving a specific book or author.</p>
     */
    Integer maxResults;
}
