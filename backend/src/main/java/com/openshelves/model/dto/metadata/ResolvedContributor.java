package com.openshelves.model.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.openshelves.model.enums.AuthorRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/// Represents a resolved contributor, which is a unified profile derived from multiple raw provider references
/// believed to be the same person. It includes the resolved attributes, any outstanding conflicts, and the
/// history of decisions made during the resolution process.
///
/// This object serves as the primary state entity exchanged between the backend (BE) and frontend (FE).
/// It is fully constructed during the initial resolution phase. On subsequent edits, the entire object is sent
/// back to the backend and re-evaluated, ensuring a stateless backend design where no session is maintained
/// between requests.
///
/// Note on `roleClaims`: The ordering is managed by the frontend. The backend accepts the provided order,
/// validating only for uniqueness and contiguous sequences, and will silently repair any validation failures
/// rather than rejecting the request.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolvedContributor {

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /// A stable identifier used for tracking the entity across backend and frontend boundaries.
    /// Note that this is not a decision-log key. When a contributor is split, the resulting entity receives
    /// a new ID; during a merge, a new ID is generated or a parent's ID is retained.
    private UUID id;

    /// The original raw provider records from which this contributor was resolved.
    @Builder.Default
    private List<ProviderAuthorRef> originalRefs = new ArrayList<>();


    // -------------------------------------------------------------------------
    // Resolved attributes
    // -------------------------------------------------------------------------

    /// The resolved display name of the contributor.
    private String name;

    /// The resolved Amazon Standard Identification Number (ASIN), if a valid candidate survived the resolution process.
    private String asin;

    /// The resolved Open Library identifier, if a valid candidate survived the resolution process.
    private String olid;


    // -------------------------------------------------------------------------
    // Roles
    // -------------------------------------------------------------------------

    /// The collection of roles attributed to this contributor. The ordering of this list is
    /// controlled by the frontend to determine presentation order.
    @Builder.Default
    private List<RoleClaim> roleClaims = new ArrayList<>();


    // -------------------------------------------------------------------------
    // Review state
    // -------------------------------------------------------------------------

    /// The list of currently unresolved conflicts associated with this contributor.
    @Builder.Default
    private List<Conflict> conflicts = new ArrayList<>();

    /// The historical log of decisions made by the user to resolve past conflicts.
    @Builder.Default
    private List<Decision> decisionHistory = new ArrayList<>();

    /// Checks whether this contributor currently has any unresolved conflicts.
    ///
    /// @return `true` if there is at least one conflict; `false` otherwise.
    public boolean hasConflicts() {
        return conflicts != null && !conflicts.isEmpty();
    }


    // ----------------------------------------------------------------
    // Nested types
    // ----------------------------------------------------------------

    /// Represents a single role assigned to this contributor, along with its specific ordering
    /// relative to other contributors who share the same role.
    /// A single contributor can hold multiple roles simultaneously (e.g., both `AUTHOR` and `ILLUSTRATOR` for the same book).
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoleClaim {

        /// The specific role assigned to the contributor (e.g., AUTHOR, ILLUSTRATOR).
        AuthorRole role;

        /// The relative position or ordering of this contributor among others holding the same role.
        Integer sortOrder;
    }

    /// Represents a single unresolved point of contention that requires user intervention.
    ///
    /// The `candidates` list is populated exclusively for conflict types where the user must select
    /// between competing values (e.g., `NAME_CONFLICT`, `CONFLICTING_IDENTIFIERS`). For informational
    /// conflict types (e.g., `ROLE_UNSPECIFIED`, `HAS_MULTI_ROLE`), this list remains empty.
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Conflict {

        /// The category of conflict requiring user intervention.
        ConflictType type;

        /// The list of competing values the user must choose between (applicable only to certain conflict types).
        @Builder.Default
        List<String> candidates = new ArrayList<>();
    }

    /// Records a user's resolution for a specific past [Conflict].
    ///
    /// During re-evaluation, a previous decision is automatically reapplied only if the `chosenValue`
    /// remains present among the newly computed candidates for the given `conflictType`. If it is no longer
    /// a valid candidate, the conflict is resurfaced to the user rather than relying on stale data.
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Decision {

        /// The specific type of conflict that was resolved.
        ConflictType conflictType;

        /// The value selected by the user to resolve the conflict.
        String chosenValue;
    }

    /// Defines the specific types of conflicts that can emerge after the initial resolution phase.
    /// These conflicts specifically require explicit user decisions; any conflict resolved automatically
    /// during phase 1 is not represented here.
    public enum ConflictType {

        /// Indicates that multiple valid candidates survived the resolution process for the contributor's name.
        NAME_CONFLICT,
        /// Indicates that multiple valid candidates survived for an external identifier (e.g., ASIN, OLID).
        CONFLICTING_IDENTIFIERS,
        /// Indicates that no specific role has been assigned to the contributor.
        ROLE_UNSPECIFIED,
        /// Indicates that an unusually high number of distinct roles (four or more) have been assigned to the same contributor.
        HAS_MULTI_ROLE
    }
}
