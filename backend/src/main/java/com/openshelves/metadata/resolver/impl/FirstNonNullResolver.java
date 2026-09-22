package com.openshelves.metadata.resolver.impl;

import com.openshelves.metadata.models.FieldResolution;
import com.openshelves.metadata.entity.FieldResolutionPolicyEntity;
import com.openshelves.metadata.enums.MetadataField;
import com.openshelves.metadata.enums.MetadataProvider;
import com.openshelves.metadata.enums.ResolutionStrategy;
import com.openshelves.metadata.resolver.MetadataFieldResolver;
import org.springframework.stereotype.Component;

import java.util.Map;

/// Resolves metadata fields by returning the first non-null candidate value in [MetadataProvider] enum declaration order.
///
/// This strategy is used when any available value is acceptable and provider precedence does not matter.
@Component
public class FirstNonNullResolver implements MetadataFieldResolver {

    @Override
    public ResolutionStrategy strategy() {
        return ResolutionStrategy.FIRST_NON_NULL;
    }

    @Override
    public <T> FieldResolution<T> resolve(MetadataField field, Map<MetadataProvider, T> candidates, FieldResolutionPolicyEntity policy) {
        // Look for the first non-null value
        for (MetadataProvider provider : MetadataProvider.values()) {
            T value = candidates.get(provider);
            if (value != null) {
                return new FieldResolution.Resolved<>(value);
            }
        }

        return new FieldResolution.Absent<>();
    }
}
