package com.openshelves.services.metadata.resolver;

import com.openshelves.model.dto.metadata.FieldResolution;
import com.openshelves.model.entity.FieldResolutionPolicyEntity;
import com.openshelves.model.enums.MetadataField;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.model.enums.ResolutionStrategy;
import com.openshelves.services.metadata.MetadataFieldResolver;
import org.springframework.stereotype.Component;

import java.util.Map;

/// Resolves metadata fields by returning the first non-null candidate value
/// in the natural iteration order of the candidates map.
///
/// This strategy acts as a basic fallback when provider ordering or priority ranking
/// is not specified by the policy.
@Component
public class FirstNonNullResolver implements MetadataFieldResolver {

    @Override
    public ResolutionStrategy strategy() {
        return ResolutionStrategy.FIRST_NON_NULL;
    }

    // TODO: iterate candidates in map order, return the first non-null value as Resolved,
    //       or Absent if all candidates are null
    @Override
    public <T> FieldResolution<T> resolve(MetadataField field, Map<MetadataProvider, T> candidates, FieldResolutionPolicyEntity policy) {
        throw new UnsupportedOperationException("FirstNonNullResolver is not yet implemented");
    }
}
