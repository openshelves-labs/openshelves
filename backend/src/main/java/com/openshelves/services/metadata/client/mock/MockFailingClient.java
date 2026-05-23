package com.openshelves.services.metadata.client.mock;

import com.openshelves.model.dto.metadata.*;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.services.metadata.MetadataClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * A mock implementation of {@link MetadataClient} that always fails.
 *
 * <p>This implementation is designed to test error handling, fallback logic,
 * and Resilience4j patterns (retries, rate limiting) in the metadata service layer.</p>
 */
@Component
public class MockFailingClient implements MetadataClient {

    @Override
    public MetadataProvider provider() {
        return MetadataProvider.AMAZON;
    }

    @Override
    public List<ExternalBook> fetchBooks(BookRequest request, FetchOptions fetchOptions) {
        throw new RuntimeException("Simulated service failure from Amazon Metadata Provider (Mock)");
    }

    @Override
    public List<ExternalAuthor> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
        throw new RuntimeException("Simulated service failure from Amazon Metadata Provider (Mock)");
    }
}
