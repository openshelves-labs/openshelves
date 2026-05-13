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
 * A mock implementation of {@link MetadataClient} simulating the Google Books API.
 *
 * <p>This client provides a static set of classic literature to facilitate the development
 * and testing of the book discovery flow without requiring external API keys or
 * network connectivity.</p>
 *
 * <p>Search behavior:
 * <ul>
 *   <li><b>ID Search:</b> If an ISBN, ASIN, or OLID is provided, it returns exactly one matching book.</li>
 *   <li><b>Title Search:</b> Returns the full mock dataset to simulate multiple search results.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class MockGoogleBooksClient implements MetadataClient {

    private static final List<ExternalBook> BOOK_DATA = new ArrayList<>();
    private static final List<ExternalAuthor> AUTHOR_DATA = new ArrayList<>();

    static {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String booksJson = """
                [
                  {
                    "title": "The Great Gatsby",
                    "description": "A 1925 novel by American writer F. Scott Fitzgerald.",
                    "language": "en",
                    "pageCount": 180,
                    "publisher": "Scribner",
                    "publicationYear": 1925,
                    "isbn10": "0743273567",
                    "isbn13": "9780743273565",
                    "authors": [{ "name": "F. Scott Fitzgerald", "role": "AUTHOR" }]
                  },
                  {
                    "title": "1984",
                    "subtitle": "A Novel",
                    "description": "Dystopian social science fiction novel by English novelist George Orwell.",
                    "language": "en",
                    "pageCount": 328,
                    "publisher": "Secker & Warburg",
                    "publicationYear": 1949,
                    "isbn10": "0451524934",
                    "isbn13": "9780451524935",
                    "asin": "B003JTHWKU",
                    "authors": [{ "name": "George Orwell", "role": "AUTHOR" }]
                  },
                  {
                    "title": "Animal Farm",
                    "description": "A satirical allegorical novella by George Orwell.",
                    "language": "en",
                    "pageCount": 112,
                    "publisher": "Secker & Warburg",
                    "publicationYear": 1945,
                    "isbn10": "0451526341",
                    "isbn13": "9780451526342",
                    "authors": [{ "name": "George Orwell", "role": "AUTHOR" }]
                  },
                  {
                    "title": "Brave New World",
                    "description": "A dystopian social science fiction novel by Aldous Huxley.",
                    "language": "en",
                    "pageCount": 268,
                    "publisher": "Chatto & Windus",
                    "publicationYear": 1932,
                    "isbn10": "0060850523",
                    "isbn13": "9780060850524",
                    "authors": [{ "name": "Aldous Huxley", "role": "AUTHOR" }]
                  }
                ]
                """;

            String authorsJson = """
                [
                  {
                    "name": "F. Scott Fitzgerald",
                    "bio": "Francis Scott Key Fitzgerald was an American novelist.",
                    "nationality": "American",
                    "birthYear": 1896,
                    "deathYear": 1940
                  },
                  {
                    "name": "George Orwell",
                    "bio": "English novelist, essayist, journalist, and critic.",
                    "nationality": "English",
                    "birthYear": 1903,
                    "deathYear": 1950
                  }
                ]
                """;

            BOOK_DATA.addAll(mapper.readValue(booksJson, new TypeReference<List<ExternalBook>>() {}));
            AUTHOR_DATA.addAll(mapper.readValue(authorsJson, new TypeReference<List<ExternalAuthor>>() {}));
        } catch (Exception e) {
            log.error("Failed to initialize mock data for Google Books", e);
        }
    }

    @Override
    public MetadataProvider getProvider() {
        return MetadataProvider.GOOGLE_BOOKS;
    }

    @Override
    public List<ExternalBook> fetchBooks(BookRequest request, FetchOptions fetchOptions) {
        log.debug("Mock fetching books from Google Books for request: {}", request);

        // 1. Priority ID Search (Returns 1 result if matched)
        // In a real provider, ID lookups are usually specific and return a single canonical result.
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
            return List.of(); // ID specified but not found in mock data
        }

        // 2. Title Search / Default (Simulate multiple results)
        // To help in development of the UI flow, we return multiple books when a title search is performed.
        int limit = (fetchOptions != null && fetchOptions.getMaxResults() != null)
                ? Math.min(fetchOptions.getMaxResults(), BOOK_DATA.size())
                : BOOK_DATA.size();

        return BOOK_DATA.subList(0, limit);
    }

    @Override
    public List<ExternalAuthor> fetchAuthors(AuthorRequest request, FetchOptions fetchOptions) {
        log.debug("Mock fetching authors from Google Books for request: {}", request);

        int limit = (fetchOptions != null && fetchOptions.getMaxResults() != null)
                ? Math.min(fetchOptions.getMaxResults(), AUTHOR_DATA.size())
                : AUTHOR_DATA.size();

        return AUTHOR_DATA.subList(0, limit);
    }
}
