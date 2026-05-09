package com.openshelves.services.metadata;

import com.openshelves.model.dto.metadata.ExternalBook;
import com.openshelves.model.enums.MetadataProvider;

import java.util.Optional;

/**
 * Contract for interacting with an external metadata provider.
 *
 * <p>Implementations of this interface are responsible for communicating with
 * a specific third-party service and mapping its responses into normalized,
 * provider-agnostic DTOs used by the system.</p>
 */
public interface MetadataClient {

    /**
     * Returns the metadata provider handled by this client.
     *
     * @return the associated {@link MetadataProvider}
     */
    MetadataProvider getProvider();

    /**
     * Fetches book metadata using an ISBN identifier.
     *
     * @param isbn the ISBN of the book
     * @return an {@link Optional} containing the book metadata if found
     */
    Optional<ExternalBook> fetchBookByIsbn(String isbn);

}
