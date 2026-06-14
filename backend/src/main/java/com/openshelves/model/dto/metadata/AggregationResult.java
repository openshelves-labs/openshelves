package com.openshelves.model.dto.metadata;

import com.openshelves.model.enums.MetadataField;
import lombok.NonNull;
import lombok.Value;

import java.util.Collections;
import java.util.Map;

/// Represents the result of aggregating metadata from multiple sources for an entity.
///
/// @param <T>  The type of the domain entity being aggregated (e.g., Book, Author).
@Value
public class AggregationResult<T> {

    // -------------------------------------------------------------------------
    // Data fields
    // -------------------------------------------------------------------------

    /// The partially or fully aggregated entity instance. This may be `null` if the
    /// entity could not be instantiated or if critical fields are blocked.
    T entity;

    /// A map containing fields that could not be automatically resolved and require
    /// manual review or confirmation. Keys are the specific [MetadataField]s, and
    /// values are the corresponding [FieldResolution] states detailing the
    /// resolution attempt.
    @NonNull
    Map<MetadataField, FieldResolution<?>> needsReview;


    // -------------------------------------------------------------------------
    // named Constructors
    // -------------------------------------------------------------------------

    /// Creates an empty aggregation result representing no data retrieved.
    ///
    /// @param <T> the type of the domain entity
    /// @return an empty [AggregationResult] instance
    public static <T> AggregationResult<T> empty() {
        return new AggregationResult<>(null, Collections.emptyMap());
    }


    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    /// Checks if the aggregation result is empty, meaning no provider returned
    /// any value for the entity being aggregated.
    ///
    /// @return `true` if no provider returned any value (entity is null); `false` otherwise
    public boolean isEmpty() {
        return entity == null;
    }

    /// Checks whether there are any metadata fields that require manual review or
    /// contain unresolved conflicts.
    ///
    /// @return `true` if there are unresolved fields requiring review; `false` otherwise
    public boolean hasConflicts() {
        return !needsReview.isEmpty();
    }
}
