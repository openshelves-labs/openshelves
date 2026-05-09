package com.openshelves.services.metadata;

import com.openshelves.model.enums.MetadataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry that maps each {@link MetadataProvider} to its corresponding {@link MetadataClient}
 * implementation.
 *
 * <p>All {@code MetadataClient} beans present in the application context are collected at startup
 * and indexed by their declared provider. The registry is immutable after construction; clients
 * cannot be added or removed at runtime.
 *
 * <p>Enforces a one-to-one relationship between providers and clients: if two beans claim the same
 * {@link MetadataProvider}, the application will fail to start with an {@link IllegalStateException}.
 */
@Slf4j
@Component
public class MetadataClientRegistry {

    private final Map<MetadataProvider, MetadataClient> clients;

    /**
     * Constructs the registry by indexing all available {@link MetadataClient} beans.
     *
     * @param allClients all {@code MetadataClient} implementations discovered in the application
     *                   context; Spring injects these automatically via list injection
     *
     * @throws IllegalStateException if two clients declare the same {@link MetadataProvider}
     */
    @Autowired
    public MetadataClientRegistry(List<MetadataClient> allClients) {
        Map<MetadataProvider, MetadataClient> clientMap = new EnumMap<>(MetadataProvider.class);
        for (MetadataClient client : allClients) {
            MetadataProvider provider = client.getProvider();
            MetadataClient existing = clientMap.put(provider, client);
            if (existing != null) {
                throw new IllegalStateException(
                    "Duplicate MetadataClient for provider: " + provider);
            }
        }
        this.clients = Collections.unmodifiableMap(clientMap);

        log.debug("Registered MetadataClients: {}", clients.keySet());
    }

    /**
     * Returns the {@link MetadataClient} registered for the given provider.
     *
     * @param provider the metadata provider to look up
     * @return the client associated with {@code provider}; never {@code null}
     * @throws IllegalArgumentException if no client is registered for the given provider
     */
    public MetadataClient getClient(MetadataProvider provider) {
        MetadataClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalArgumentException(
                "No MetadataClient registered for provider: " + provider);
        }
        return client;
    }
}
