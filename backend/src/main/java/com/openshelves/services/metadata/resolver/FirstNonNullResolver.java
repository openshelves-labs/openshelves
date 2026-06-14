package com.openshelves.services.metadata.resolver;

import com.openshelves.model.dto.metadata.FieldResolution;
import com.openshelves.model.entity.FieldResolutionPolicyEntity;
import com.openshelves.model.enums.MetadataField;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.model.enums.ResolutionStrategy;
import com.openshelves.services.metadata.MetadataFieldResolver;
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
