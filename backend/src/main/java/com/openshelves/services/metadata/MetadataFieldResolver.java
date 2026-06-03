package com.openshelves.services.metadata;

import com.openshelves.model.dto.metadata.FieldResolution;
import com.openshelves.model.entity.FieldResolutionPolicyEntity;
import com.openshelves.model.enums.MetadataField;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.model.enums.ResolutionStrategy;

import java.util.Map;

/// Defines the contract for resolving conflicting metadata values from multiple providers
/// based on a specific [ResolutionStrategy].
///
/// Implementations of this interface encapsulate the logic required to merge, rank,
/// or aggregate candidate values for a given [MetadataField] to produce a final
/// [FieldResolution].
public interface MetadataFieldResolver {

    /// Returns the resolution strategy that this resolver implements.
    ///
    /// @return the [ResolutionStrategy] supported by this implementation
    ResolutionStrategy strategy();

    /// Resolves the final value for a metadata field given a set of candidate values
    /// from various providers and the configured resolution policy.
    ///
    /// @param field      the metadata field being resolved
    /// @param candidates a map of candidate values keyed by their source provider
    /// @param policy     the resolution policy governing this field
    /// @param <T>        the type of the metadata field value
    ///
    /// @return a [FieldResolution] indicating whether the field was successfully resolved,
    ///         requires manual confirmation, or is blocked due to an unresolvable conflict
    <T> FieldResolution<T> resolve(
        MetadataField field,
        Map<MetadataProvider, T> candidates,
        FieldResolutionPolicyEntity policy
    );
}
