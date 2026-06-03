package com.openshelves.model.dto.metadata;

import lombok.Builder;
import lombok.Value;

/// Controls the behaviour of a metadata fetch operation.
///
/// Passed alongside a request object (e.g., [BookRequest] or [AuthorRequest])
/// to tune how results are retrieved — for example, limiting the number of results or
/// filtering by language.
///
/// Instances are immutable and constructed via the builder:
/// ```java
/// FetchOptions options = FetchOptions.builder()
///     .withLanguage("fr")
///     .withMaxResults(3)
///     .build();
/// ```
@Value
@Builder(setterPrefix = "with")
public class FetchOptions {

    // -------------------------------------------------------------------------
    // Filtering
    // -------------------------------------------------------------------------

    /// ISO-639-1 language code to filter results by (e.g., `"en"`, `"fr"`).
    ///
    /// When `null`, no language filter is applied.
    String language;


    // -------------------------------------------------------------------------
    // Pagination
    // -------------------------------------------------------------------------

    /// Maximum number of results to return per fetch.
    ///
    /// When `null`, each provider applies its own default cap.
    /// For most metadata lookups a small number (e.g., `5`) is sufficient —
    /// callers rarely need a long list when resolving a specific book or author.
    Integer maxResults;
}
