package com.openshelves.model.dto.metadata;

import java.util.ArrayList;
import java.util.List;

import com.openshelves.model.enums.AuthorRole;
import com.openshelves.model.enums.MetadataProvider;

import lombok.Builder;
import lombok.Value;

/// Represents a contributor (e.g., author, illustrator, editor) associated with a book, 
/// used during the metadata review and reconciliation process.
///
/// This class aggregates contributor identifiers and tracks the roles they play across
/// different metadata providers, as well as any conflicting information found.
///
/// Instances are immutable and constructed via the Lombok builder:
/// ```java
/// ContributorReview review = ContributorReview.builder()
///     .withName("John Doe")
///     .withOlid("OL1234567A")
///     .build();
/// ```
@Value
@Builder(setterPrefix = "with")
public class ContributorReview {

    // -------------------------------------------------------------------------
    // Identifiers and Details
    // -------------------------------------------------------------------------
    /// The name of the contributor.
    String name;

    /// Open Library identifier.
    String olid;

    /// Amazon Standard Identification Number.
    String asin;

    /// Internal database primary identifier, if known.
    Long dbPid;

    // -------------------------------------------------------------------------
    // Review Data
    // -------------------------------------------------------------------------
    /// List of roles assigned to this contributor by various metadata providers.
    @Builder.Default
    List<RoleAssignment> roles = new ArrayList<>();

    /// List of conflicts detected during the metadata reconciliation process.
    @Builder.Default
    List<Conflict> conflicts = new ArrayList<>();

    /// Represents a specific role assigned to a contributor, along with the providers
    /// that reported this role.
    @Value
    @Builder(setterPrefix = "with")
    public static class RoleAssignment {

        /// The role the contributor played.
        AuthorRole role;

        /// The order of importance or billing for this role.
        int sortOrder;

        /// The metadata providers that reported this role.
        List<MetadataProvider> sources;
    }

    /// Represents a data conflict related to this contributor found during reconciliation.
    @Value
    @Builder(setterPrefix = "with")
    public static class Conflict {

        /// The type or description of the conflict.
        String type;
    }
}
