package com.openshelves.model.dto.metadata;

import com.openshelves.model.enums.MetadataProvider;
import lombok.*;

import java.util.Map;

/// Represents the result of an attempt to resolve a specific metadata field from one or more
/// external providers.
///
/// Because different providers (e.g., Google Books, Open Library) might return missing,
/// conflicting, or uncertain data, this interface models the resolution state with four
/// possible outcomes:
///
/// - [Resolved]: A definitive value was found.
/// - [NeedsConfirmation]: A value was found but it requires manual review.
/// - [Blocked]: Conflicting values were found from different providers.
/// - [Absent]: No provider could supply a value for this field.
///
/// @param <T> the type of the resolved field value
public sealed interface FieldResolution<T> {

    /// Indicates that a definitive value was successfully resolved.
    ///
    /// @param <T> the type of the resolved value
    @Getter
    @ToString
    @RequiredArgsConstructor
    final class Resolved<T> implements FieldResolution<T> {
        /// The successfully resolved value.
        private final T value;
    }

    /// Indicates that a value was proposed, but it is uncertain and requires manual confirmation.
    ///
    /// @param <T> the type of the proposed value
    @Getter
    @ToString
    @RequiredArgsConstructor
    final class NeedsConfirmation<T> implements FieldResolution<T> {
        /// The proposed value that needs confirmation.
        private final T proposedValue;

        /// The reason why confirmation is required.
        private String rationale;


        // -------------------------------------------------------------------------
        // Setter Methods
        // -------------------------------------------------------------------------

        public NeedsConfirmation<T> withRationale(String rationale) {
            this.rationale = rationale;
            return this;
        }
    }

    /// Indicates that resolution is blocked because different providers returned conflicting values.
    ///
    /// @param <T> the type of the conflicting values
    @Getter
    @ToString
    @RequiredArgsConstructor
    final class Blocked<T> implements FieldResolution<T> {
        /// A map showing the conflicting values returned by each provider.
        private final Map<MetadataProvider, T> conflictingValues;

        /// An optionally suggested value to resolve the conflict.
        private T suggestedValue;

        /// The explanation of the conflict.
        private String rationale;


        // -------------------------------------------------------------------------
        // Setter Methods
        // -------------------------------------------------------------------------

        public Blocked<T> withSuggestedValue(T suggestedValue) {
            this.suggestedValue = suggestedValue;
            return this;
        }

        public Blocked<T> withRationale(String rationale) {
            this.rationale = rationale;
            return this;
        }
    }

    /// Indicates that no provider could supply a value for this field.
    ///
    /// @param <T> the type of the expected field
    @Getter
    @ToString
    @RequiredArgsConstructor
    final class Absent<T> implements FieldResolution<T> {}
}
