package com.openshelves.services.metadata.resolver;

import com.openshelves.model.dto.metadata.FieldResolution;
import com.openshelves.model.entity.FieldResolutionPolicyEntity;
import com.openshelves.model.enums.MetadataField;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.model.enums.ResolutionStrategy;
import com.openshelves.services.metadata.MetadataFieldResolver;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RankedProviderResolver implements MetadataFieldResolver {

    @Override
    public ResolutionStrategy strategy() {
        return ResolutionStrategy.PRIORITY;
    }

    @Override
    public <T> FieldResolution<T> resolve(MetadataField field, Map<MetadataProvider, T> candidates, FieldResolutionPolicyEntity policy) {
        // Get the provider ranking from the policy
        List<MetadataProvider> rankedProviders = policy.getRankedProviders();
        if (rankedProviders == null) {
            rankedProviders = new ArrayList<>();
        }

        // ----------------------------------------------------------------
        // Phase 1: walk the ranked list — first non-null value wins
        // ----------------------------------------------------------------
        for (MetadataProvider provider : rankedProviders) {
            T value = candidates.get(provider);
            if (value != null) {
                return new FieldResolution.Resolved<>(value);
            }
        }

        // ----------------------------------------------------------------
        // Phase 2: collect unranked candidates
        // ----------------------------------------------------------------
        Set<T> unrankedValues = candidates.values().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (unrankedValues.isEmpty()) {
            return new FieldResolution.Absent<>();
        }

        // ----------------------------------------------------------------
        // Phase 3a: unanimous consensus among unranked providers
        // ----------------------------------------------------------------
        if (unrankedValues.size() == 1) {
            T consensusValue = unrankedValues.iterator().next();
            return new FieldResolution.NeedsConfirmation<>(consensusValue, null);
        }

        // ----------------------------------------------------------------
        // Phase 3b: conflict — ask AI to propose a winner
        // ----------------------------------------------------------------
        T proposedValue = null;
        FieldResolution.AiAssessment assessment = null;

        return new FieldResolution.Blocked<>(proposedValue, candidates, assessment);
    }
}
