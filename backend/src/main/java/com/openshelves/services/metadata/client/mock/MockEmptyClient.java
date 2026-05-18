package com.openshelves.services.metadata.client.mock;

import com.openshelves.model.dto.metadata.*;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.services.metadata.MetadataClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * A mock implementation of {@link MetadataClient} that always returns empty results.
 *
 * <p>This implementation is useful for testing "no results found" states in the UI
 * and ensuring the system handles empty lists gracefully without errors.</p>
 */
@Component
public class MockEmptyClient implements MetadataClient {

    @Override
    public MetadataProvider getProvider() {
        return MetadataProvider.GOODREADS;
    }

    @Override
    public List<ExternalBook> fetchBooks(BookRequest request, FetchOptions fetchOptions) {
        return Collections.emptyList();
    }

    @Override
    public List<ExternalAuthor> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
        return Collections.emptyList();
    }
}
