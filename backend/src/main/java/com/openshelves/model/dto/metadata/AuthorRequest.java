package com.openshelves.model.dto.metadata;

import lombok.Builder;
import lombok.Value;

/// Encapsulates the search criteria used to query author metadata from an external provider.
///
/// All fields are optional — callers should populate whichever identifiers or
/// descriptors are available. Implementations of [com.openshelves.services.metadata.MetadataClient]
/// are responsible for deciding which fields to use and in what priority order.
///
/// Instances are immutable and constructed via the Lombok builder:
/// ```java
/// AuthorRequest request = AuthorRequest.builder()
///     .withOlid("OL23919A")
///     .withName("J.R.R. Tolkien")
///     .build();
/// ```
@Value
@Builder(setterPrefix = "with")
public class AuthorRequest {

    // -------------------------------------------------------------------------
    // Descriptive fields
    // -------------------------------------------------------------------------

    /// Display name of the author to search for.
    String name;


    // -------------------------------------------------------------------------
    // Identifiers
    // -------------------------------------------------------------------------

    /// Amazon identifier for the author. Takes priority over name when present.
    String asin;

    /// Open Library identifier for the author. Takes priority over name when present.
    String olid;
}
