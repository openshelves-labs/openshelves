package com.openshelves.services.metadata.resolver;

import com.openshelves.model.dto.metadata.FieldResolution;
import com.openshelves.model.entity.FieldResolutionPolicyEntity;
import com.openshelves.model.enums.MetadataField;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.model.enums.ResolutionStrategy;
import com.openshelves.services.metadata.MetadataFieldResolver;
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
        return null;
    }
}
