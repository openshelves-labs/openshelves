package com.openshelves.services.metadata;

import com.openshelves.model.enums.MetadataField;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry that maps entity classes to the set of metadata fields they support.
 *
 * <p>This registry is initialized at startup by scanning the {@link MetadataField} enum
 * and grouping fields by their associated entity type (e.g., {@link com.openshelves.model.entity.BookEntity}).
 * It provides a centralized way to discover which fields are available for metadata operations
 * on a given entity.</p>
 */
@Slf4j
@Component
public class MetadataFieldRegistry {

    /**
     * Internal mapping of entity types to their supported metadata fields.
     */
    private static final Map<Class<?>, EnumSet<MetadataField>> registry;

    static {
        // Group all MetadataField enum entries by their target entity class
        Map<Class<?>, List<MetadataField>> fieldMap = Arrays.stream(MetadataField.values())
            .collect(
                Collectors.groupingBy(MetadataField::getEntityType)
            );

        // Convert the lists to EnumSets and make the map immutable for thread-safe read access
        registry = fieldMap.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                e -> EnumSet.copyOf(e.getValue())
            ));
    }

    /**
     * Retrieves the set of metadata fields registered for a specific entity type.
     *
     * @param entityType the class of the entity to look up (e.g., {@code BookEntity.class})
     * @return an {@link EnumSet} of supported {@link MetadataField}s
     * @throws IllegalArgumentException if no fields are registered for the given entity type
     */
    public EnumSet<MetadataField> getFieldsForEntity(Class<?> entityType) {
        EnumSet<MetadataField> fields = registry.get(entityType);
        if (fields == null) {
            throw new IllegalArgumentException("No metadata fields registered for entity type: " + entityType.getName());
        }
        return fields;
    }
}
