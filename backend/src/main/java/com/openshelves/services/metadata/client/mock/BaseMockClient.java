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
        if (hasAnyId(request)) {
            return findBookById(request);
        }

        // 2. Title Search / Default (Simulate multiple results)
        int limit = (fetchOptions != null && fetchOptions.getMaxResults() != null)
                ? Math.min(fetchOptions.getMaxResults(), getBookData().size())
                : getBookData().size();

        return getBookData().subList(0, limit);
    }

    private boolean hasAnyId(BookRequest request) {
        return request.getIsbn13() != null || request.getIsbn10() != null
                || request.getOlid() != null || request.getAsin() != null;
    }

    private List<ExternalBook> findBookById(BookRequest request) {
        for (ExternalBook book : getBookData()) {
            if (matchesId(book, request)) {
                return List.of(book);
            }
        }
        return List.of();
    }

    private boolean matchesId(ExternalBook book, BookRequest request) {
        if (request.getIsbn13() != null && request.getIsbn13().equals(book.getIsbn13())) return true;
        if (request.getIsbn10() != null && request.getIsbn10().equals(book.getIsbn10())) return true;
        if (request.getOlid() != null && request.getOlid().equals(book.getOlid())) return true;
        if (request.getAsin() != null && request.getAsin().equals(book.getAsin())) return true;
        return false;
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
