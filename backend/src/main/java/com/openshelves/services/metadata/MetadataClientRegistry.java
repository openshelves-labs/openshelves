package com.openshelves.services.metadata;

import com.openshelves.model.enums.MetadataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/// Registry that maps each [MetadataProvider] to its corresponding [MetadataClient]
/// implementation.
///
/// All `MetadataClient` beans present in the application context are collected at startup
/// and indexed by their declared provider. The registry is immutable after construction; clients
/// cannot be added or removed at runtime.
///
/// Enforces a one-to-one relationship between providers and clients: if two beans claim the same
/// [MetadataProvider], the application will fail to start with an [IllegalStateException].
@Slf4j
@Component
public class MetadataClientRegistry {

    private final Map<MetadataProvider, MetadataClient> registry;

    /// Constructs the registry by indexing all available [MetadataClient] beans.
    ///
    /// @param allClients all `MetadataClient` implementations discovered in the application
    ///                   context; Spring injects these automatically via list injection
    ///
    /// @throws IllegalStateException if two clients declare the same [MetadataProvider]
    @Autowired
    public MetadataClientRegistry(List<MetadataClient> allClients) {
        Map<MetadataProvider, MetadataClient> providerMap = new EnumMap<>(MetadataProvider.class);
        for (MetadataClient client : allClients) {
            MetadataProvider provider = client.provider();
            MetadataClient existing = providerMap.put(provider, client);
            if (existing != null) {
                throw new IllegalStateException(
                    "Duplicate MetadataClient for provider: " + provider);
            }
        }
        this.registry = Collections.unmodifiableMap(providerMap);

        log.debug("Registered MetadataClients: {}", registry.keySet());
    }

    /// Returns the [MetadataClient] registered for the given provider.
    ///
    /// @param provider the metadata provider to look up
    ///
    /// @return the client associated with `provider`; never `null`
    /// @throws IllegalArgumentException if no client is registered for the given provider
    public MetadataClient getClient(MetadataProvider provider) {
        MetadataClient client = registry.get(provider);
        if (client == null) {
            throw new IllegalArgumentException(
                "No MetadataClient registered for provider: " + provider);
        }
        return client;
    }

    /// Returns a read-only map of all registered metadata clients.
    ///
    /// @return a map of all registered [MetadataProvider]s to their corresponding [MetadataClient]s
    public Map<MetadataProvider, MetadataClient> getAllClients() {
        return registry;
    }
}
