package com.openshelves.model.enums;

import com.openshelves.model.dto.metadata.ExternalBook;
import com.openshelves.services.metadata.MetadataFieldAccessor;
import lombok.Getter;

/**
 * Enumeration of all metadata fields supported by the system.
 *
 * <p>Each entry maps a logical metadata field (e.g., {@code BOOK_TITLE}) to its
 * physical representation in an entity class. It also encapsulates a
 * {@link MetadataFieldAccessor} for reflective access and optional provider locking.</p>
 */
@Getter
public enum MetadataField {

    /** The primary title of a book. */
    BOOK_TITLE("title", String.class, ExternalBook.class);

    /** The entity type associated with this metadata field. */
    private final Class<?> entityType;

    /** Low-level accessor for reading and writing this field on entity instances. */
    private final MetadataFieldAccessor accessor;

    /** The specific provider restricted to supplying values for this field, if any. */
    private final MetadataProvider lockedProvider;

    /**
     * Constructs a metadata field that can be supplied by any provider.
     *
     * @param fieldName  the name of the field in the entity class
     * @param fieldType  the Java type of the field
     * @param entityType the entity class containing the field
     */
    MetadataField(String fieldName, Class<?> fieldType, Class<?> entityType) {
        this(fieldName, fieldType, entityType, null);
    }

    /**
     * Constructs a metadata field that is locked to a specific provider.
     *
     * @param fieldName      the name of the field in the entity class
     * @param fieldType      the Java type of the field
     * @param entityType     the entity class containing the field
     * @param lockedProvider the only provider allowed to supply this field
     */
    MetadataField(String fieldName, Class<?> fieldType, Class<?> entityType, MetadataProvider lockedProvider) {
        this.entityType = entityType;
        this.accessor = new MetadataFieldAccessor(fieldName, fieldType, entityType);
        this.lockedProvider = lockedProvider;
    }

    /**
     * Indicates whether this field is restricted to a single authoritative provider.
     *
     * @return {@code true} if the field can only be supplied by a specific source
     */
    public boolean isProviderLocked() {
        return lockedProvider != null;
    }
}
