package com.openshelves.metadata.model;

import com.openshelves.metadata.enums.AuthorRole;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder(setterPrefix = "with")
@Jacksonized
public class BookMetadata {

    // -------------------------------------------------------------------------
    // Core metadata
    // -------------------------------------------------------------------------

    /// Primary title of the book.
    String title;

    /// Secondary or extended title (e.g., subtitle or tagline).
    String subtitle;

    /// Description or summary of the book content.
    String description;

    /// Language code (typically ISO-639-1, e.g., "en").
    String language;

    /// Total number of pages, if available.
    Integer pageCount;


    // -------------------------------------------------------------------------
    // Classification
    // -------------------------------------------------------------------------

    /// Dewey Decimal Classification (DDC) code.
    String deweyDecimal;

    /// Library of Congress classification code.
    String lcClassification;


    // -------------------------------------------------------------------------
    // Publisher
    // -------------------------------------------------------------------------

    /// Name of the publishing entity.
    String publisher;

    /// Year the book was published.
    Short publicationYear;


    // -------------------------------------------------------------------------
    // Series
    // -------------------------------------------------------------------------

    /// Name of the series this book belongs to.
    String seriesName;

    /// Position of the book within the series.
    Integer seriesNumber;


    // -------------------------------------------------------------------------
    // Identifiers
    // -------------------------------------------------------------------------

    /// ISBN-10 identifier.
    String isbn10;

    /// ISBN-13 identifier.
    String isbn13;

    /// Amazon Standard Identification Number.
    String asin;

    /// Open Library identifier.
    String olid;


    // -------------------------------------------------------------------------
    // Media
    // -------------------------------------------------------------------------

    /// URL pointing to the book's cover image.
    String coverImageUrl;


    // -------------------------------------------------------------------------
    // Relationships
    // -------------------------------------------------------------------------

    /// Lightweight references to contributors associated with the book.
    ///
    /// Only minimal identifying information is included here. Full author
    /// details are expected to be resolved separately.
    @Builder.Default
    List<AuthorRef> authors = new ArrayList<>();


    /// Lightweight reference to a contributor (e.g., author, editor, translator).
    ///
    /// This is not a full author representation—only the minimum data required
    /// to establish a relationship between a book and a contributor.
    @Value
    @Builder(setterPrefix = "with")
    @Jacksonized
    public static class AuthorRef {

        // -------------------------------------------------------------------------
        // Core metadata
        // -------------------------------------------------------------------------

        /// Display name of the contributor.
        String name;


        // -------------------------------------------------------------------------
        // Identifiers
        // -------------------------------------------------------------------------

        /// Amazon identifier for the contributor, if available.
        String asin;

        /// Open Library identifier for the contributor.
        String olid;


        // -------------------------------------------------------------------------
        // Additional Metadata
        // -------------------------------------------------------------------------

        /// Role of the contributor (e.g., AUTHOR, EDITOR, TRANSLATOR).
        @Builder.Default
        AuthorRole role = AuthorRole.AUTHOR;
    }
}
