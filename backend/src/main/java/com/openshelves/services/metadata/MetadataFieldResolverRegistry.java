package com.openshelves.services.metadata;

import com.openshelves.model.enums.ResolutionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry that maps each {@link ResolutionStrategy} to its corresponding {@link MetadataFieldResolver}
 * implementation.
 *
 * <p>All {@code MetadataFieldResolver} beans present in the application context are collected at startup
 * and indexed by their declared strategy. The registry is immutable after construction; resolvers
 * cannot be added or removed at runtime.
 *
 * <p>Enforces a one-to-one relationship between strategies and resolvers: if two beans claim the same
 * {@link ResolutionStrategy}, the application will fail to start with an {@link IllegalStateException}.
 */
@Slf4j
@Component
public class MetadataFieldResolverRegistry {

    private final Map<ResolutionStrategy, MetadataFieldResolver> registry;

    /**
     * Constructs the registry by indexing all available {@link MetadataFieldResolver} beans.
     *
     * @param allResolvers all {@code MetadataFieldResolver} implementations discovered in the application
     *                   context; Spring injects these automatically via list injection
     *
     * @throws IllegalStateException if two resolvers declare the same {@link ResolutionStrategy}
     */
    @Autowired
    public MetadataFieldResolverRegistry(List<MetadataFieldResolver> allResolvers) {
        Map<ResolutionStrategy, MetadataFieldResolver> strategyMap = new EnumMap<>(ResolutionStrategy.class);
        for (MetadataFieldResolver resolver : allResolvers) {
            ResolutionStrategy strategy = resolver.strategy();
            MetadataFieldResolver existing = strategyMap.put(strategy, resolver);
            if (existing != null) {
                throw new IllegalStateException(
                    "Duplicate MetadataFieldResolver for strategy: " + strategy);
            }
        }
        this.registry = Collections.unmodifiableMap(strategyMap);

        log.debug("Registered MetadataFieldResolvers: {}", registry.keySet());
    }

    /**
     * Returns the {@link MetadataFieldResolver} registered for the given strategy.
     *
     * @param strategy the resolution strategy to look up
     * @return the resolver associated with {@code strategy}; never {@code null}
     * @throws IllegalArgumentException if no resolver is registered for the given strategy
     */
    public MetadataFieldResolver getResolver(ResolutionStrategy strategy) {
        MetadataFieldResolver resolver = registry.get(strategy);
        if (resolver == null) {
            throw new IllegalArgumentException(
                "No MetadataFieldResolver registered for strategy: " + strategy);
        }
        return resolver;
    }
}
