package com.openshelves.model.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.openshelves.model.enums.AuthorRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/// Represents normalized book metadata retrieved from external providers
/// such as Google Books or Open Library.
///
/// This DTO is intentionally tolerant and minimal:
///
///   - All fields are optional and may be `null`
///   - Unknown properties from upstream APIs are ignored
///   - `null` fields are excluded from JSON serialization
///
/// The structure is designed for ingestion and mapping, not as a
/// fully authoritative domain model.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalBook {

    // -------------------------------------------------------------------------
    // Core metadata
    // -------------------------------------------------------------------------

    /// Primary title of the book.
    private String title;

    /// Secondary or extended title (e.g., subtitle or tagline).
    private String subtitle;

    /// Description or summary of the book content.
    private String description;

    /// Language code (typically ISO-639-1, e.g., "en").
    private String language;

    /// Total number of pages, if available.
    private Integer pageCount;


    // -------------------------------------------------------------------------
    // Classification
    // -------------------------------------------------------------------------

    /// Dewey Decimal Classification (DDC) code.
    private String deweyDecimal;

    /// Library of Congress classification code.
    private String lcClassification;


    // -------------------------------------------------------------------------
    // Publisher
    // -------------------------------------------------------------------------

    /// Name of the publishing entity.
    private String publisher;

    /// Year the book was published.
    private Short publicationYear;


    // -------------------------------------------------------------------------
    // Series
    // -------------------------------------------------------------------------

    /// Name of the series this book belongs to.
    private String seriesName;

    /// Position of the book within the series.
    private Integer seriesNumber;


    // -------------------------------------------------------------------------
    // Identifiers
    // -------------------------------------------------------------------------

    /// ISBN-10 identifier.
    private String isbn10;

    /// ISBN-13 identifier.
    private String isbn13;

    /// Amazon Standard Identification Number.
    private String asin;

    /// Open Library identifier.
    private String olid;


    // -------------------------------------------------------------------------
    // Media
    // -------------------------------------------------------------------------

    /// URL pointing to the book's cover image.
    private String coverImageUrl;


    // -------------------------------------------------------------------------
    // Relationships
    // -------------------------------------------------------------------------

    /// Lightweight references to contributors associated with the book.
    ///
    /// Only minimal identifying information is included here. Full author
    /// details are expected to be resolved separately.
    @Builder.Default
    private List<AuthorRef> authors = new ArrayList<>();


    /// Lightweight reference to a contributor (e.g., author, editor, translator).
    ///
    /// This is not a full author representation—only the minimum data required
    /// to establish a relationship between a book and a contributor.
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthorRef {

        // -------------------------------------------------------------------------
        // Core metadata
        // -------------------------------------------------------------------------

        /// Display name of the contributor.
        private String name;


        // -------------------------------------------------------------------------
        // Identifiers
        // -------------------------------------------------------------------------

        /// Amazon identifier for the contributor, if available.
        private String asin;

        /// Open Library identifier for the contributor.
        private String olid;


        // -------------------------------------------------------------------------
        // Additional Metadata
        // -------------------------------------------------------------------------

        /// Role of the contributor (e.g., AUTHOR, EDITOR, TRANSLATOR).
        private AuthorRole role;
    }
}
