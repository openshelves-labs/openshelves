package com.openshelves.services.metadata.resolver.roleassignment;

import com.openshelves.model.dto.metadata.ExternalBook;
import com.openshelves.model.dto.metadata.ProviderAuthorRef;
import com.openshelves.model.enums.AuthorRole;
import com.openshelves.model.enums.MetadataProvider;
import org.springframework.stereotype.Component;

import java.util.*;

/// Transforms a collection of author references grouped by their source [MetadataProvider]
/// into a single, flattened list of [ProviderAuthorRef] objects.
///
/// This mapper also ensures that authors within the same provider and role
/// are assigned a 1-based sort order to maintain deterministic ordering.
@Component
public class ProviderAuthorRefMapper {

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /// Flattens a map of author references from multiple providers into a single list.
    ///
    /// @param authorRefs A map of author references grouped by their source provider.
    /// @return A flat list of [ProviderAuthorRef] objects with assigned sort orders.
    public List<ProviderAuthorRef> flattenAuthorRefs(Map<MetadataProvider, List<ExternalBook.AuthorRef>> authorRefs) {
        if (authorRefs == null || authorRefs.isEmpty()) {
            return List.of();
        }

        return authorRefs.entrySet().stream()
            .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
            .flatMap(entry -> toProviderAuthorRefs(entry.getKey(), entry.getValue()).stream())
            .toList();
    }

    // ----------------------------------------------------------------
    // Helper Methods
    // ----------------------------------------------------------------

    private List<ProviderAuthorRef> toProviderAuthorRefs(MetadataProvider provider, List<ExternalBook.AuthorRef> authorRefs) {
        if (authorRefs == null || authorRefs.isEmpty()) {
            return List.of();
        }

        Map<AuthorRole, Integer> sortOrderCounter = new EnumMap<>(AuthorRole.class);
        List<ProviderAuthorRef> result = new ArrayList<>();

        for (ExternalBook.AuthorRef authorRef : authorRefs) {
            if (authorRef == null) {
                continue;
            }

            ProviderAuthorRef providerAuthorRef = ProviderAuthorRef.builder()
                .authorRef(authorRef)
                .provider(provider)
                .sortOrder(nextSortOrder(sortOrderCounter, authorRef.getRole()))
                .build();

            result.add(providerAuthorRef);
        }

        return result;
    }

    private Integer nextSortOrder(Map<AuthorRole, Integer> sortOrderCounter, AuthorRole authorRole) {
        if (authorRole == null) {
            return 1;      // No ordering needed for unmapped authors
        }
        return sortOrderCounter.merge(authorRole, 1, Integer::sum);   // 1-based indexing
    }
}
