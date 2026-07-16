package com.openshelves.services.metadata.resolver;

import com.openshelves.model.dto.metadata.FieldResolution;
import com.openshelves.model.entity.FieldResolutionPolicyEntity;
import com.openshelves.model.enums.MetadataField;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.model.enums.ResolutionStrategy;
import com.openshelves.services.metadata.MetadataFieldResolver;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/// Resolves metadata fields by bypassing automatic resolution and directly
/// blocking the field, requiring explicit manual review or operator input.
///
/// This strategy is typically used for critical field parameters that must
/// be audited manually before publication.
@Component
public class ManualResolver implements MetadataFieldResolver {

    @Override
    public ResolutionStrategy strategy() {
        return ResolutionStrategy.MANUAL;
    }

    @Override
    public <T> FieldResolution<T> resolve(MetadataField field, Map<MetadataProvider, T> candidates, FieldResolutionPolicyEntity policy) {

        Set<T> distinctValues = candidates.values().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (distinctValues.isEmpty()) {
            return new FieldResolution.Absent<>();
        }

        // Providers agree, so there's no conflict to adjudicate — but the
        // field is still policy-gated, so a reviewer confirms rather than
        // choosing between values.
        if (distinctValues.size() == 1) {
            return new FieldResolution.NeedsConfirmation<>(distinctValues.iterator().next());
        }

        // Providers disagree; a reviewer has to pick or supply a value.
        return new FieldResolution.Blocked<>(candidates);
    }
}
