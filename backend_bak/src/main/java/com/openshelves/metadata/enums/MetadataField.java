package com.openshelves.metadata.enums;

import lombok.Getter;

/// Enumeration of all metadata fields supported by the system.
///
/// Each entry represents a specific piece of metadata that can be retrieved from external providers,
/// such as book titles, author names, publication years, and identifiers like ISBN or ASIN.
/// The enum also includes information about whether a field is locked to a specific provider.
@Getter
public enum MetadataField {

    // -------------------------------------------------------------------------
    // Book Fields
    // -------------------------------------------------------------------------

    /// The name of a book. Includes the main title and any subtitle, if present.
    BOOK_TITLE("Book title"),

    /// A description or summary of the book.
    BOOK_DESCRIPTION("Book description"),

    /// The language code (e.g., `en`) of the book.
    BOOK_LANGUAGE("Book language"),

    /// The total number of pages in the book.
    BOOK_PAGE_COUNT("Book page count"),

    /// The Dewey Decimal Classification code.
    BOOK_DEWEY_DECIMAL("Dewey Decimal classification"),

    /// The Library of Congress classification code.
    BOOK_LC_CLASSIFICATION("Library of Congress classification"),

    /// The name of the publisher.
    BOOK_PUBLISHER("Book publisher"),

    /// The year the book was published.
    BOOK_PUBLICATION_YEAR("Book publication year"),

    /// The name and number of the book series, if applicable.
    BOOK_SERIES_NAME("Book series info"),

    /// The ISBN-10 identifier.
    BOOK_ISBN10("Book ISBN-10"),

    /// The ISBN-13 identifier.
    BOOK_ISBN13("Book ISBN-13"),

    /// The Amazon Standard Identification Number for the book.
    BOOK_ASIN("Book ASIN"),

    /// The Open Library identifier for the book.
    BOOK_OLID("Book OLID"),

    /// The URL of the book's cover image.
    BOOK_COVER_IMAGE_URL("Book cover image URL"),

    /// The list of contributors associated with the book.
    BOOK_AUTHORS("Book authors list"),


    // -------------------------------------------------------------------------
    // Author Fields
    // -------------------------------------------------------------------------

    /// The display name of the author.
    AUTHOR_NAME("Author name"),

    /// A biography or description of the author.
    AUTHOR_BIO("Author bio"),

    /// The nationality of the author.
    AUTHOR_NATIONALITY("Author nationality"),

    /// The year the author was born.
    AUTHOR_BIRTH_YEAR("Author birth year"),

    /// The year the author died.
    AUTHOR_DEATH_YEAR("Author death year"),

    /// The Amazon identifier for the author.
    AUTHOR_ASIN("Author ASIN"),

    /// The Open Library identifier for the author.
    AUTHOR_OLID("Author OLID"),

    /// The URL of the author's profile image.
    AUTHOR_PROFILE_IMAGE_URL("Author profile image URL");


    // -------------------------------------------------------------------------
    // Structure
    // -------------------------------------------------------------------------

    /// A human-readable name for the field, used in logs and error messages.
    private final String name;

    /// The specific provider restricted to supplying values for this field, if any.
    private final MetadataProvider lockedProvider;


    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /// Constructs a metadata field that can be supplied by any provider.
    MetadataField(String name) {
        this(name, null);
    }

    /// Constructs a metadata field that is locked to a specific provider.
    MetadataField(String name, MetadataProvider lockedProvider) {
        this.name = name;
        this.lockedProvider = lockedProvider;
    }


    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /// Indicates whether this field is restricted to a single authoritative provider.
    ///
    /// @return `true` if the field can only be supplied by a specific source
    public boolean isProviderLocked() {
        return lockedProvider != null;
    }
}
