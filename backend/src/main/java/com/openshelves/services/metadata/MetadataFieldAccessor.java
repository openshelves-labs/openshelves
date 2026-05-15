package com.openshelves.services.metadata;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Low-level utility for reflective access to entity fields using {@link MethodHandle}.
 *
 * <p>Provides a type-safe and performant way to get and set metadata fields on entity
 * instances without the overhead of traditional reflection on every call. Accessors
 * are resolved once during construction.</p>
 */
public class MetadataFieldAccessor {

    private final String fieldName;
    private final MethodHandle getter;
    private final MethodHandle setter;

    /**
     * Constructs an accessor for a specific field on an entity type.
     *
     * @param fieldName  the name of the field to access
     * @param fieldType  the Java type of the field
     * @param entityType the class of the entity containing the field
     * @throws IllegalArgumentException if the field cannot be found or accessed
     */
    public MetadataFieldAccessor(String fieldName, Class<?> fieldType, Class<?> entityType) {
        this.fieldName = fieldName;

        // Initialize Accessors
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(entityType, MethodHandles.lookup());
            this.getter = lookup.findGetter(entityType, fieldName, fieldType);
            this.setter = lookup.findSetter(entityType, fieldName, fieldType);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalArgumentException("Failed to resolve accessors for field '" + fieldName + "' on " + entityType.getSimpleName(), e);
        }
    }

    /**
     * Retrieves the value of the field from the given entity instance.
     *
     * @param <T>    the expected return type
     * @param entity the entity instance to read from
     * @return the value of the field
     * @throws RuntimeException if the value cannot be retrieved
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Object entity) {
        try {
            return (T) getter.invoke(entity);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to get value for field '" + fieldName + "' on " + entity.getClass().getSimpleName(), e);
        }
    }

    /**
     * Sets the value of the field on the given entity instance.
     *
     * @param entity the entity instance to modify
     * @param value  the new value to set
     * @throws RuntimeException if the value cannot be set
     */
    public void set(Object entity, Object value) {
        try {
            setter.invoke(entity, value);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to set value for field '" + fieldName + "' on " + entity.getClass().getSimpleName(), e);
        }
    }
}
