package com.openshelves.services.metadata.client.mock;

import com.openshelves.model.dto.metadata.*;
import com.openshelves.services.metadata.MetadataClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Base class for mock metadata clients that provides common search and filtering logic.
 */
public abstract class BaseMockClient implements MetadataClient {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * @return the mock book data to search against
     */
    protected abstract List<ExternalBook> getBookData();

    /**
     * @return the mock author data to search against
     */
    protected abstract List<ExternalAuthor> getAuthorData();

    @Override
    public List<ExternalBook> fetchBooks(BookRequest request, FetchOptions fetchOptions) {
        log.debug("Mock fetching books from {} for request: {}", provider(), request);

        // 1. Priority ID Search (Returns 1 result if matched)
        String isbn13 = request.getIsbn13();
        String isbn10 = request.getIsbn10();
        String olid = request.getOlid();
        String asin = request.getAsin();

        if (isbn13 != null || isbn10 != null || olid != null || asin != null) {
            for (ExternalBook book : getBookData()) {
                if ((isbn13 != null && isbn13.equals(book.getIsbn13())) ||
                    (isbn10 != null && isbn10.equals(book.getIsbn10())) ||
                    (olid != null && olid.equals(book.getOlid())) ||
                    (asin != null && asin.equals(book.getAsin()))) {
                    return List.of(book);
                }
            }
            return List.of();
        }

        // 2. Title Search / Default (Simulate multiple results)
        int limit = (fetchOptions != null && fetchOptions.getMaxResults() != null)
                ? Math.min(fetchOptions.getMaxResults(), getBookData().size())
                : getBookData().size();

        return getBookData().subList(0, limit);
    }

    @Override
    public List<ExternalAuthor> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
        log.debug("Mock fetching authors from {} for request: {}", provider(), request);

        int limit = (fetchOptions != null && fetchOptions.getMaxResults() != null)
                ? Math.min(fetchOptions.getMaxResults(), getAuthorData().size())
                : getAuthorData().size();

        return getAuthorData().subList(0, limit);
    }
}
