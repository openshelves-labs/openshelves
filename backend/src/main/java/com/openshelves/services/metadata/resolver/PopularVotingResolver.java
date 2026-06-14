package com.openshelves.services.metadata.resolver;

import com.openshelves.model.dto.metadata.FieldResolution;
import com.openshelves.model.entity.FieldResolutionPolicyEntity;
import com.openshelves.model.enums.MetadataField;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.model.enums.ResolutionStrategy;
import com.openshelves.services.metadata.MetadataFieldResolver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/// Resolves metadata fields using a voting mechanism.
///
/// This strategy counts occurrences of each unique candidate value across all
/// providers and selects the one with the majority vote. Ties or low consensus
/// are flagged according to the configured resolution policy.
@Component
public class PopularVotingResolver implements MetadataFieldResolver {

    // A plurality winner must hold more than this share of the non-null votes
    // to be auto-resolved. Below this, it surfaces for human confirmation.
    public static final double MINIMUM_VOTE_SHARE = 0.5;

    @Override
    public ResolutionStrategy strategy() {
        return ResolutionStrategy.VOTING;
    }

    @Override
    public <T> FieldResolution<T> resolve(MetadataField field, Map<MetadataProvider, T> candidates, FieldResolutionPolicyEntity policy) {
        // Count occurrences of each candidate value
        Map<T, Long> voteCounts = candidates.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()));

        // Calculate total votes
        long totalVotes = voteCounts.values().stream().mapToLong(Long::longValue).sum();

        if (totalVotes == 0) {
            return new FieldResolution.Absent<>();
        }

        // Find the candidate with the most votes
        long maxVotes = voteCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);

        List<T> topCandidates = voteCounts.entrySet().stream()
                .filter(entry -> entry.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .toList();

        // Check for ties or low consensus
        if (topCandidates.size() > 1) {
            return new FieldResolution.Blocked<>(candidates);
        }

        T selectedValue = topCandidates.getFirst();
        double consensus = (double) maxVotes / totalVotes;

        if (consensus > MINIMUM_VOTE_SHARE) {
            return new FieldResolution.Resolved<>(selectedValue);
        } else {
            return new FieldResolution.NeedsConfirmation<>(selectedValue);
        }
    }
}
