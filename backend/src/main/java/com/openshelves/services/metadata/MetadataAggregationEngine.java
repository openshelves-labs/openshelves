package com.openshelves.services.metadata;

import com.openshelves.model.dto.metadata.*;
import com.openshelves.model.entity.FieldResolutionPolicyEntity;
import com.openshelves.model.enums.MetadataField;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.model.enums.ResolutionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/// Core engine responsible for orchestrating the aggregation of metadata from multiple
/// external providers.
///
/// Fetches data from all registered providers, resolves conflicts at the field level
/// using defined policies, and constructs a unified domain entity representing the
/// aggregated result.
@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataAggregationEngine {

    private final MetadataClientRegistry clientRegistry;
    private final MetadataFieldRegistry fieldRegistry;
    private final MetadataFieldResolverRegistry resolverRegistry;
    private final MetadataFieldPolicyService policyService;


    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /// Aggregates book metadata by fetching data from all registered providers and
    /// resolving field conflicts.
    ///
    /// @param request      The book request containing identifiers and search criteria.
    /// @param fetchOptions Options governing the fetch behavior (e.g., bypassing cache).
    ///
    /// @return an [AggregationResult] containing the resolved [ExternalBook] and any
    ///         fields needing review
    public AggregationResult<ExternalBook> aggregateBook(BookRequest request, FetchOptions fetchOptions) {
        Map<MetadataProvider, ExternalBook> providerResults = fetchFromAllProviders(
            request,
            client -> client.fetchBooks(request, fetchOptions)
        );

        if (providerResults.isEmpty()) {
            log.debug("No providers returned data for book request {}", request);
            return AggregationResult.empty();
        }

        return resolveEntity(providerResults, ExternalBook.class, ExternalBook::new);
    }

    /// Aggregates author metadata by fetching data from all registered providers and
    /// resolving field conflicts.
    ///
    /// @param request      The author request containing identifiers and search criteria.
    /// @param fetchOptions Options governing the fetch behavior (e.g., bypassing cache).
    ///
    /// @return an [AggregationResult] containing the resolved [ExternalAuthor] and any
    ///         fields needing review
    public AggregationResult<ExternalAuthor> aggregateAuthor(AuthorRequest request, FetchOptions fetchOptions) {
        Map<MetadataProvider, ExternalAuthor> providerResults = fetchFromAllProviders(
            request,
            client -> client.fetchAuthors(request, fetchOptions)
        );

        if (providerResults.isEmpty()) {
            log.debug("No providers returned data for author request {}", request);
            return AggregationResult.empty();
        }

        return resolveEntity(providerResults, ExternalAuthor.class, ExternalAuthor::new);
    }


    // -----------------------------------------------------------------------
    // Fetch
    // -----------------------------------------------------------------------

    /// Executes the fetch operation across all registered providers, collecting the
    /// first available result from each.
    ///
    /// Provider failures are caught, logged, and skipped — ensuring they never abort
    /// the aggregation pipeline.
    ///
    /// @param request The request object being processed (used primarily for logging).
    /// @param fetcher A function that takes a MetadataClient and returns a list of results.
    /// @param <T>     The type of the result returned by the fetcher.
    ///
    /// @return a map containing the first valid result from each successful provider
    private <T> Map<MetadataProvider, T> fetchFromAllProviders(Object request, Function<MetadataClient, List<T>> fetcher) {
        Map<MetadataProvider, MetadataClient> clients = clientRegistry.getAllClients();
        Map<MetadataProvider, T> results = new EnumMap<>(MetadataProvider.class);

        clients.forEach((provider, client) -> {
            try {
                List<T> providerResults = fetcher.apply(client);
                if (!providerResults.isEmpty()) {
                    results.put(provider, providerResults.getFirst());
                }
            } catch (Exception e) {
                log.warn("Provider {} failed — skipping: {}", provider, e.getMessage());
            }
        });

        log.debug("Fetch complete. {} of {} providers returned data for: {}", results.size(), clients.size(), request);

        return results;
    }


    // -----------------------------------------------------------------------
    // Resolution
    // -----------------------------------------------------------------------

    /// Orchestrates the resolution of an entity by applying field-level resolution
    /// policies to provider candidates.
    ///
    /// @param providerResults A map of successful metadata responses keyed by their respective providers.
    /// @param entityType      The class type of the target entity being resolved.
    /// @param entityFactory   A supplier that instantiates a new instance of the target entity.
    /// @param <T>             The type of the target entity.
    ///
    /// @return an [AggregationResult] containing the newly populated entity and a map
    ///         of unresolved fields
    private <T> AggregationResult<T> resolveEntity(
        Map<MetadataProvider, T> providerResults,
        Class<T> entityType,
        Supplier<T> entityFactory
    ) {
        EnumSet<MetadataField> fields = fieldRegistry.getFieldsForEntity(entityType);

        // Bulk-load all applicable policies in a single query
        Map<MetadataField, FieldResolutionPolicyEntity> policies = policyService.bulkLoadPolicies(fields);

        T newEntity = entityFactory.get();
        Map<MetadataField, FieldResolution<?>> needsReview = new EnumMap<>(MetadataField.class);

        for (MetadataField field : fields) {
            Map<MetadataProvider, ?> candidates = extractCandidates(field, providerResults);
            FieldResolutionPolicyEntity policy = policies.get(field);
            if (policy == null) {
                throw new IllegalStateException("No policies found for field: " + field);
            }

            FieldResolution<?> resolution = resolveField(field, candidates, policy);

            if (resolution instanceof FieldResolution.Resolved<?> resolvedResolution) {
                applyResolution(newEntity, field, resolvedResolution);
                continue;
            }

            if (resolution instanceof FieldResolution.Absent<?>) {
                continue;       // Absent is a valid resolution state indicating no value; it does not require review
            }

            needsReview.put(field, resolution);
        }

        if (!needsReview.isEmpty()) {
            log.debug("Aggregation completed with {} field(s) requiring manual review", needsReview.size());
        }

        return new AggregationResult<>(newEntity, Collections.unmodifiableMap(needsReview));
    }

    /// Resolves a single metadata field from a pool of candidate values according to the configured policy.
    ///
    /// @param field      The metadata field being resolved.
    /// @param candidates A map of provider candidates for the given field.
    /// @param policy     The resolution policy defining the strategy and provider priorities for the field.
    ///
    /// @return The final [FieldResolution] which is either resolved or requires manual review.
    private FieldResolution<?> resolveField(MetadataField field, Map<MetadataProvider, ?> candidates, FieldResolutionPolicyEntity policy) {
        // Fields locked to a specific provider bypass the resolver entirely
        if (field.isProviderLocked()) {
            Object value = candidates.get(field.getLockedProvider());
            if (value == null) {
                return new FieldResolution.Absent<>();
            }
            return new FieldResolution.Resolved<>(value);
        }

        // No provider had a value — resolve to absent immediately, no resolver needed
        if (candidates.isEmpty()) {
            return new FieldResolution.Absent<>();    // Absent signifies "no value"
        }

        ResolutionStrategy strategy = policy.getResolutionStrategy();
        MetadataFieldResolver resolver = resolverRegistry.getResolver(strategy);
        return resolver.resolve(field, candidates, policy);
    }

    /// Extracts non-null candidate values for a specific metadata field from all
    /// successful provider results.
    ///
    /// @param field           The metadata field to extract.
    /// @param providerResults The aggregated provider results.
    /// @param <T>             The type of the entity containing the field.
    ///
    /// @return a map of candidate values keyed by the provider that supplied them
    private <T> Map<MetadataProvider, ?> extractCandidates(MetadataField field, Map<MetadataProvider, T> providerResults) {
        Map<MetadataProvider, Object> candidates = new EnumMap<>(MetadataProvider.class);

        providerResults.forEach((provider, entity) -> {
            Object value = field.getAccessor().get(entity);
            if (value != null) {
                candidates.put(provider, value);
            }
        });

        return candidates;
    }

    /// Applies a successfully resolved value to the target entity using the field's accessor.
    ///
    /// @param newEntity          The target entity being populated.
    /// @param field              The metadata field being populated.
    /// @param resolvedResolution The resolved value wrapper.
    /// @param <T>                The type of the target entity.
    private <T> void applyResolution(T newEntity, MetadataField field, FieldResolution.Resolved<?> resolvedResolution) {
        Object value = resolvedResolution.value();
        if (value != null) {
            field.getAccessor().set(newEntity, value);
        }
    }
}
