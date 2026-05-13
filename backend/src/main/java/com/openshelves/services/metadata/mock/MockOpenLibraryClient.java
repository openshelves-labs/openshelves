package com.openshelves.services.metadata.mock;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.openshelves.model.dto.metadata.*;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.services.metadata.MetadataClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A mock implementation of {@link MetadataClient} simulating Open Library.
 *
 * <p>This client provides a static set of fantasy literature. Some books (like *The Great Gatsby*)
 * overlap with other mock providers but with different metadata, allowing for testing of
 * provider-specific merging logic.</p>
 *
 * <p>Search behavior:
 * <ul>
 *   <li><b>ID Search:</b> Returns a single matching book for ISBN, ASIN, or OLID.</li>
 *   <li><b>Title Search:</b> Returns the full mock dataset to simulate multiple hits.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class MockOpenLibraryClient implements MetadataClient {

    private static final List<ExternalBook> BOOK_DATA = new ArrayList<>();
    private static final List<ExternalAuthor> AUTHOR_DATA = new ArrayList<>();

    static {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String booksJson = """
                [
                  {
                    "title": "The Great Gatsby",
                    "description": "A classic of 20th-century literature, this novel explores themes of decadence and idealism.",
                    "language": "en",
                    "pageCount": 218,
                    "publisher": "Charles Scribner's Sons",
                    "publicationYear": 1925,
                    "isbn10": "0743273567",
                    "isbn13": "9780743273565",
                    "olid": "OL12345W",
                    "authors": [{ "name": "F. Scott Fitzgerald", "role": "AUTHOR", "olid": "OL54321A" }]
                  },
                  {
                    "title": "The Hobbit",
                    "subtitle": "There and Back Again",
                    "description": "A children's fantasy novel by English author J. R. R. Tolkien.",
                    "language": "en",
                    "pageCount": 310,
                    "publisher": "George Allen & Unwin",
                    "publicationYear": 1937,
                    "isbn10": "000752549X",
                    "isbn13": "9780007525492",
                    "olid": "OL26242W",
                    "asin": "0261102214",
                    "authors": [{ "name": "J.R.R. Tolkien", "role": "AUTHOR", "olid": "OL26320A" }]
                  },
                  {
                    "title": "The Fellowship of the Ring",
                    "seriesName": "The Lord of the Rings",
                    "seriesNumber": 1,
                    "description": "The first volume of J. R. R. Tolkien's epic fantasy novel.",
                    "language": "en",
                    "pageCount": 423,
                    "publisher": "George Allen & Unwin",
                    "publicationYear": 1954,
                    "isbn10": "0618346252",
                    "isbn13": "9780618346257",
                    "authors": [{ "name": "J.R.R. Tolkien", "role": "AUTHOR" }]
                  },
                  {
                    "title": "The Two Towers",
                    "seriesName": "The Lord of the Rings",
                    "seriesNumber": 2,
                    "description": "The second volume of J. R. R. Tolkien's epic fantasy novel.",
                    "language": "en",
                    "pageCount": 352,
                    "publisher": "George Allen & Unwin",
                    "publicationYear": 1954,
                    "isbn10": "0618346260",
                    "isbn13": "9780618346264",
                    "authors": [{ "name": "J.R.R. Tolkien", "role": "AUTHOR" }]
                  }
                ]
                """;

            String authorsJson = """
                [
                  {
                    "name": "J.R.R. Tolkien",
                    "bio": "John Ronald Reuel Tolkien was an English writer and academic.",
                    "nationality": "English",
                    "birthYear": 1892,
                    "deathYear": 1973,
                    "olid": "OL26320A"
                  }
                ]
                """;

            BOOK_DATA.addAll(mapper.readValue(booksJson, new TypeReference<List<ExternalBook>>() {}));
            AUTHOR_DATA.addAll(mapper.readValue(authorsJson, new TypeReference<List<ExternalAuthor>>() {}));
        } catch (Exception e) {
            log.error("Failed to initialize mock data for Open Library", e);
        }
    }

    @Override
    public MetadataProvider getProvider() {
        return MetadataProvider.OPEN_LIBRARY;
    }

    @Override
    public List<ExternalBook> fetchBooks(BookRequest request, FetchOptions fetchOptions) {
        log.debug("Mock fetching books from Open Library for request: {}", request);

        // 1. Priority ID Search (Returns 1 result if matched)
        String isbn13 = request.getIsbn13();
        String isbn10 = request.getIsbn10();
        String olid = request.getOlid();
        String asin = request.getAsin();

        if (isbn13 != null || isbn10 != null || olid != null || asin != null) {
            for (ExternalBook book : BOOK_DATA) {
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
                ? Math.min(fetchOptions.getMaxResults(), BOOK_DATA.size())
                : BOOK_DATA.size();

        return BOOK_DATA.subList(0, limit);
    }

    @Override
    public List<ExternalAuthor> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
        log.debug("Mock fetching authors from Open Library for request: {}", request);

        int limit = (fetchOptions != null && fetchOptions.getMaxResults() != null)
                ? Math.min(fetchOptions.getMaxResults(), AUTHOR_DATA.size())
                : AUTHOR_DATA.size();

        return AUTHOR_DATA.subList(0, limit);
    }
}
