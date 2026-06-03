package com.openshelves.model.dto.metadata;

import com.openshelves.model.enums.MetadataProvider;

import java.util.Map;

/// Represents the outcome of attempting to resolve a metadata field value
/// from one or more external data providers.
///
/// This sealed interface defines the possible states resulting from the metadata
/// resolution process. It is used to encapsulate whether a field was cleanly resolved,
/// requires human review, or encountered an unresolvable conflict.
///
/// @param <T> the type of the metadata field value
public sealed interface FieldResolution<T>
    permits FieldResolution.Resolved, FieldResolution.NeedsConfirmation, FieldResolution.Blocked, FieldResolution.Absent {

    /// Indicates that the field was successfully resolved to a single, unambiguous value.
    /// This occurs when all providers agree, or a provider with sufficiently high priority
    /// provides the value according to the configured resolution policy.
    ///
    /// @param value the final, resolved value for the metadata field
    /// @param <T>   the type of the resolved value
    record Resolved<T>(
        T value
    ) implements FieldResolution<T> {}

    /// Indicates that a proposed value was determined, but requires manual confirmation
    /// before it can be finalized. This state is typically triggered when the AI assessment
    /// yields a confidence score below the required threshold for automatic resolution.
    ///
    /// @param proposedValue the most likely value determined by the resolution engine
    /// @param assessment    the AI-generated reasoning and confidence score for this proposal
    /// @param <T>           the type of the proposed value
    record NeedsConfirmation<T>(
        T proposedValue,
        AiAssessment assessment
    ) implements FieldResolution<T> {}

    /// Indicates that the field resolution is blocked due to conflicting values from
    /// multiple providers, and the system cannot safely determine the correct value.
    /// This state demands human intervention to resolve the conflict.
    ///
    /// @param proposedValue     a tentatively proposed value, if one could be generated
    /// @param conflictingValues a mapping of metadata providers to their respective conflicting values
    /// @param assessment        the AI-generated context explaining the conflict and why it could not be resolved
    /// @param <T>               the type of the proposed and conflicting values
    record Blocked<T>(
        T proposedValue,
        Map<MetadataProvider, T> conflictingValues,
        AiAssessment assessment
    ) implements FieldResolution<T> {}

    /// Indicates that no value could be found for the field from any provider.
    /// This occurs when all providers return null or are missing the field.
    ///
    /// @param <T> the type of the absent value
    record Absent<T>() implements FieldResolution<T> {}

    /// Encapsulates the results of an AI-based evaluation during the metadata resolution process.
    /// This is used to provide transparency into how resolution decisions were made,
    /// particularly when manual confirmation is needed or a conflict occurs.
    ///
    /// @param reasoning       a human-readable explanation of how the AI evaluated the available data
    /// @param confidenceScore a normalized score (e.g., 0.0 to 1.0) indicating the system's confidence in the result
    record AiAssessment(
        String reasoning,
        double confidenceScore
    ) {}
}
