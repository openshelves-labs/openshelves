package com.openshelves.model.enums;

import com.openshelves.model.dto.metadata.ExternalAuthor;
import com.openshelves.model.dto.metadata.ExternalBook;
import com.openshelves.services.metadata.MetadataFieldAccessor;
import lombok.Getter;

import java.util.List;

/// Enumeration of all metadata fields supported by the system.
///
/// Each entry maps a logical metadata field (e.g., `BOOK_TITLE`) to its
/// physical representation in an entity class. It also encapsulates a
/// [MetadataFieldAccessor] for reflective access and optional provider locking.
@Getter
public enum MetadataField {

    // -------------------------------------------------------------------------
    // Book Fields
    // -------------------------------------------------------------------------

    /// The primary title of a book.
    BOOK_TITLE("title", String.class, ExternalBook.class),

    /// The secondary title or subtitle of a book.
    BOOK_SUBTITLE("subtitle", String.class, ExternalBook.class),

    /// A description or summary of the book.
    BOOK_DESCRIPTION("description", String.class, ExternalBook.class),

    /// The language code (e.g., `en`) of the book.
    BOOK_LANGUAGE("language", String.class, ExternalBook.class),

    /// The total number of pages in the book.
    BOOK_PAGE_COUNT("pageCount", Integer.class, ExternalBook.class),

    /// The Dewey Decimal Classification code.
    BOOK_DEWEY_DECIMAL("deweyDecimal", String.class, ExternalBook.class),

    /// The Library of Congress classification code.
    BOOK_LC_CLASSIFICATION("lcClassification", String.class, ExternalBook.class),

    /// The name of the publisher.
    BOOK_PUBLISHER("publisher", String.class, ExternalBook.class),

    /// The year the book was published.
    BOOK_PUBLICATION_YEAR("publicationYear", Short.class, ExternalBook.class),

    /// The name of the series the book belongs to.
    BOOK_SERIES_NAME("seriesName", String.class, ExternalBook.class),

    /// The position of the book within a series.
    BOOK_SERIES_NUMBER("seriesNumber", Integer.class, ExternalBook.class),

    /// The ISBN-10 identifier.
    BOOK_ISBN10("isbn10", String.class, ExternalBook.class),

    /// The ISBN-13 identifier.
    BOOK_ISBN13("isbn13", String.class, ExternalBook.class),

    /// The Amazon Standard Identification Number for the book.
    BOOK_ASIN("asin", String.class, ExternalBook.class),

    /// The Open Library identifier for the book.
    BOOK_OLID("olid", String.class, ExternalBook.class),

    /// The URL of the book's cover image.
    BOOK_COVER_IMAGE_URL("coverImageUrl", String.class, ExternalBook.class),

    /// The list of contributors associated with the book.
    BOOK_AUTHORS("authors", List.class, ExternalBook.class),


    // -------------------------------------------------------------------------
    // Author Fields
    // -------------------------------------------------------------------------

    /// The display name of the author.
    AUTHOR_NAME("name", String.class, ExternalAuthor.class),

    /// A biography or description of the author.
    AUTHOR_BIO("bio", String.class, ExternalAuthor.class),

    /// The nationality of the author.
    AUTHOR_NATIONALITY("nationality", String.class, ExternalAuthor.class),

    /// The year the author was born.
    AUTHOR_BIRTH_YEAR("birthYear", Short.class, ExternalAuthor.class),

    /// The year the author died.
    AUTHOR_DEATH_YEAR("deathYear", Short.class, ExternalAuthor.class),

    /// The Amazon identifier for the author.
    AUTHOR_ASIN("asin", String.class, ExternalAuthor.class),

    /// The Open Library identifier for the author.
    AUTHOR_OLID("olid", String.class, ExternalAuthor.class),

    /// The URL of the author's profile image.
    AUTHOR_PROFILE_IMAGE_URL("profileImageUrl", String.class, ExternalAuthor.class);


    // -------------------------------------------------------------------------
    // Structure
    // -------------------------------------------------------------------------

    /// The entity type associated with this metadata field.
    private final Class<?> entityType;

    /// Low-level accessor for reading and writing this field on entity instances.
    private final MetadataFieldAccessor accessor;

    /// The specific provider restricted to supplying values for this field, if any.
    private final MetadataProvider lockedProvider;


    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /// Constructs a metadata field that can be supplied by any provider.
    ///
    /// @param fieldName  the name of the field in the entity class
    /// @param fieldType  the Java type of the field
    /// @param entityType the entity class containing the field
    MetadataField(String fieldName, Class<?> fieldType, Class<?> entityType) {
        this(fieldName, fieldType, entityType, null);
    }

    /// Constructs a metadata field that is locked to a specific provider.
    ///
    /// @param fieldName      the name of the field in the entity class
    /// @param fieldType      the Java type of the field
    /// @param entityType     the entity class containing the field
    /// @param lockedProvider the only provider allowed to supply this field
    MetadataField(String fieldName, Class<?> fieldType, Class<?> entityType, MetadataProvider lockedProvider) {
        this.entityType = entityType;
        this.accessor = new MetadataFieldAccessor(fieldName, fieldType, entityType);
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
