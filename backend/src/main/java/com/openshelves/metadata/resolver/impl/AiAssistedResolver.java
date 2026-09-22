package com.openshelves.metadata.resolver.impl;

import com.openshelves.metadata.models.FieldResolution;
import com.openshelves.metadata.entity.FieldResolutionPolicyEntity;
import com.openshelves.metadata.enums.MetadataField;
import com.openshelves.metadata.enums.MetadataProvider;
import com.openshelves.metadata.enums.ResolutionStrategy;
import com.openshelves.metadata.resolver.MetadataFieldResolver;
import org.springframework.stereotype.Component;

import java.util.Map;

/// Resolves metadata fields by using an external LLM / AI service to synthesize
/// the candidate values.
///
/// This strategy is typically used for complex fields or fields where multiple
/// conflicting, detailed records must be merged or structured cleanly.
@Component
public class AiAssistedResolver implements MetadataFieldResolver {

    @Override
    public ResolutionStrategy strategy() {
        return ResolutionStrategy.AI_SYNTHESIZE;
    }

    @Override
    public <T> FieldResolution<T> resolve(MetadataField field, Map<MetadataProvider, T> candidates, FieldResolutionPolicyEntity policy) {
        // TODO: Implement AI-assisted resolution logic
        throw new UnsupportedOperationException("AiAssistedResolver is not yet implemented");
    }
}
