package com.openshelves.metadata.model;

import com.openshelves.metadata.client.MetadataClient;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/// Encapsulates the search criteria used to query book metadata from an external provider.
///
/// All fields are optional — callers should populate whichever identifiers or
/// descriptors are available. Implementations of [MetadataClient]
/// are responsible for deciding which fields to use and in what priority order.
///
/// Instances are immutable and constructed via the Lombok builder:
/// ```java
/// BookRequest request = BookRequest.builder()
///     .withIsbn13("9780131872486")
///     .withTitle("The C Programming Language")
///     .build();
/// ```
@Value
@Builder(setterPrefix = "with")
public class BookRequest {

    // -------------------------------------------------------------------------
    // Descriptive fields
    // -------------------------------------------------------------------------

    /// Title of the book to search for.
    String title;

    /// List of author names associated with the book.
    ///
    /// Used as a supplementary filter alongside other identifiers.
    List<String> authors;


    // -------------------------------------------------------------------------
    // Identifiers
    // -------------------------------------------------------------------------

    /// ISBN-10 identifier. Takes priority over title/author when present.
    String isbn10;

    /// ISBN-13 identifier. Takes priority over title/author when present.
    String isbn13;

    /// Amazon Standard Identification Number.
    String asin;

    /// Open Library identifier.
    String olid;
}
