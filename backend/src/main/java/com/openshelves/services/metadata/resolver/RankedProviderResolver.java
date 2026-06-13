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

/// Resolves metadata fields by walking through a prioritized list of providers.
///
/// The resolution logic follows three distinct phases:
///
/// 1. **Priority List Walk:** Iterates through the list of ranked providers in order of
///    priority. The first non-null candidate value found is returned immediately as
///    a [FieldResolution.Resolved] value.
///
/// 2. **Unranked Candidate Assessment:** If no ranked provider returned a value, gathers
///    all non-null candidate values from unranked providers.
///
/// 3. **Consensus & Fallback Resolution:**
///    - If there is unanimous consensus (exactly one unique value among all unranked
///      providers), that value is proposed with a [FieldResolution.NeedsConfirmation] state.
///
///    - If there are conflicting values among unranked providers, the field resolution
///      is marked as [FieldResolution.Blocked] for human or AI conflict resolution.
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
        // Phase 2: collect unranked candidates (no data acquired from ranked providers)
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
            return new FieldResolution.NeedsConfirmation<>(consensusValue);
        }

        // ----------------------------------------------------------------
        // Phase 3b: conflict — ask AI to propose a winner
        // ----------------------------------------------------------------
        // TODO: Call the AI Conflict Resolver with the list of unranked candidates and their sources

        return new FieldResolution.Blocked<>(candidates);
    }
}
