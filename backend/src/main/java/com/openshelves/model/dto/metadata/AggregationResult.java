package com.openshelves.model.dto.metadata;

import com.openshelves.model.enums.MetadataField;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Represents the result of aggregating metadata from multiple sources for an entity.
 *
 * @param <T> The type of the domain entity being aggregated (e.g., Book, Author).
 * @param entity The partially or fully aggregated entity instance. This may be {@code null}
 *               if the entity could not be instantiated or if critical fields are blocked.
 * @param needsReview A map containing fields that could not be automatically resolved and
 *                    require manual review or confirmation. Keys are the specific
 *                    {@link MetadataField}s, and values are the corresponding
 *                    {@link FieldResolution} states detailing the resolution attempt.
 */
public record AggregationResult<T>(
    @Nullable T entity,
    Map<MetadataField, FieldResolution<?>> needsReview
) {

    public static <T> AggregationResult<T> empty() {
        return new AggregationResult<>(null, Map.of());
    }

    /**
     * Checks if the aggregation result is empty, meaning no provider returned
     * any value for the entity being aggregated.
     *
     * @return {@code true} if no provider returned any value (entity is null); {@code false} otherwise
     */
    public boolean isEmpty() {
        return entity == null;
    }

    /**
     * Checks whether there are any metadata fields that require manual review or
     * contain unresolved conflicts.
     *
     * @return {@code true} if there are unresolved fields requiring review; {@code false} otherwise
     */
    public boolean hasConflicts() {
        return !needsReview.isEmpty();
    }
}
