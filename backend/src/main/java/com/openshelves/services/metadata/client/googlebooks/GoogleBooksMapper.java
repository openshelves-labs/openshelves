package com.openshelves.services.metadata.client.googlebooks;

import com.openshelves.model.dto.metadata.ExternalBook;
import com.openshelves.model.enums.AuthorRole;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Maps raw Google Books API JSON responses to OpenShelves domain objects.
///
/// The Google Books "volumes" endpoint returns each match as a `{id, volumeInfo, ...}`
/// object. This mapper only ever looks at the `volumeInfo` sub-object, since that's where
/// all bibliographic data lives; `saleInfo` and `accessInfo` are ignored as out of scope.
///
/// **Key responsibilities:**
/// - Extracting ISBN-10/ISBN-13 from the `industryIdentifiers` array.
/// - Parsing a publication year out of the loosely formatted `publishedDate` field
///   (which may be a bare year, `YYYY-MM`, or a full `YYYY-MM-DD`).
/// - Mapping the flat `authors` name array to [ExternalBook.AuthorRef]s for use *within* a
///   book result — this is distinct from author lookup, which Google Books does not support
///   (see [GoogleBooksClient#fetchAuthors]).
/// - Preferring the largest available cover thumbnail and upgrading it to HTTPS.
///
/// @see ExternalBook
@Component
public class GoogleBooksMapper {

    /// Matches a leading 4-digit year in a `publishedDate` value such as `"2003"`,
    /// `"2003-05"`, or `"2003-05-14"`.
    private static final Pattern YEAR_PATTERN = Pattern.compile("^(\\d{4})");

    /// Maps a single Google Books `volumeInfo` node to an [ExternalBook].
    ///
    /// @param volumeInfo the `volumeInfo` object of a Google Books volume resource
    /// @return the normalized [ExternalBook]
    public ExternalBook toExternalBook(JsonNode volumeInfo) {
        ExternalBook.ExternalBookBuilder book = ExternalBook.builder();

        book.title(text(volumeInfo, "title"));
        book.subtitle(text(volumeInfo, "subtitle"));
        book.description(text(volumeInfo, "description"));
        book.language(text(volumeInfo, "language"));
        book.publisher(text(volumeInfo, "publisher"));

        if (volumeInfo.has("pageCount") && !volumeInfo.get("pageCount").isNull()) {
            book.pageCount(volumeInfo.get("pageCount").asInt());
        }

        book.publicationYear(extractPublicationYear(text(volumeInfo, "publishedDate")));

        String[] isbns = extractIsbns(volumeInfo.get("industryIdentifiers"));
        book.isbn10(isbns[0]);
        book.isbn13(isbns[1]);

        book.coverImageUrl(extractCoverImageUrl(volumeInfo.get("imageLinks")));

        book.authors(extractAuthorRefs(volumeInfo.get("authors")));

        return book.build();
    }

    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    // Safely extracts a text field from a node, returning null if absent or JSON-null.
    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asString();
    }

    // Parses the leading 4-digit year out of a loosely formatted publishedDate string.
    private Short extractPublicationYear(String publishedDate) {
        if (StringUtils.isBlank(publishedDate)) {
            return null;
        }

        Matcher matcher = YEAR_PATTERN.matcher(publishedDate);
        if (matcher.find()) {
            return Short.parseShort(matcher.group(1));
        }
        return null;
    }

    // Extracts [isbn10, isbn13] from the industryIdentifiers array. Either slot may be null.
    private String[] extractIsbns(JsonNode industryIdentifiers) {
        String[] result = new String[2];
        if (industryIdentifiers == null || !industryIdentifiers.isArray()) {
            return result;
        }

        for (JsonNode identifier : industryIdentifiers) {
            String type = text(identifier, "type");
            String value = text(identifier, "identifier");
            if (type == null || value == null) {
                continue;
            }

            switch (type) {
                case "ISBN_10" -> result[0] = value;
                case "ISBN_13" -> result[1] = value;
                default -> { /* ignore OTHER / ISSN identifiers */ }
            }
        }
        return result;
    }

    // Picks the largest available thumbnail and upgrades it to HTTPS (Google Books
    // returns http:// links by default, which browsers will block as mixed content).
    private String extractCoverImageUrl(JsonNode imageLinks) {
        if (imageLinks == null) {
            return null;
        }

        String url = text(imageLinks, "extraLarge");
        if (url == null) url = text(imageLinks, "large");
        if (url == null) url = text(imageLinks, "medium");
        if (url == null) url = text(imageLinks, "small");
        if (url == null) url = text(imageLinks, "thumbnail");
        if (url == null) url = text(imageLinks, "smallThumbnail");

        return url != null ? url.replaceFirst("^http://", "https://") : null;
    }

    // Maps the flat "authors" string array to lightweight AuthorRefs, all tagged AUTHOR
    // since Google Books does not distinguish contributor roles.
    private List<ExternalBook.AuthorRef> extractAuthorRefs(JsonNode authors) {
        List<ExternalBook.AuthorRef> refs = new ArrayList<>();
        if (authors == null || !authors.isArray()) {
            return refs;
        }

        for (JsonNode author : authors) {
            if (author.isNull()) continue;
            refs.add(ExternalBook.AuthorRef.builder()
                .name(author.asString())
                .role(AuthorRole.AUTHOR)
                .build());
        }
        return refs;
    }
}
