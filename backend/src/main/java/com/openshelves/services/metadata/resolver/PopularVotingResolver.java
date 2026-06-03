package com.openshelves.services.metadata.resolver;

import com.openshelves.model.dto.metadata.FieldResolution;
import com.openshelves.model.entity.FieldResolutionPolicyEntity;
import com.openshelves.model.enums.MetadataField;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.model.enums.ResolutionStrategy;
import com.openshelves.services.metadata.MetadataFieldResolver;
import org.springframework.stereotype.Component;

import java.util.Map;

/// Resolves metadata fields using a voting mechanism.
///
/// This strategy counts occurrences of each unique candidate value across all
/// providers and selects the one with the majority vote. Ties or low consensus
/// are flagged according to the configured resolution policy.
@Component
public class PopularVotingResolver implements MetadataFieldResolver {

    @Override
    public ResolutionStrategy strategy() {
        return ResolutionStrategy.VOTING;
    }

    @Override
    public <T> FieldResolution<T> resolve(MetadataField field, Map<MetadataProvider, T> candidates, FieldResolutionPolicyEntity policy) {
        return null;
    }
}
