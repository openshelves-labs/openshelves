package com.openshelves.services.metadata.client.ranobedb;

import com.openshelves.model.dto.metadata.ExternalBook;
import com.openshelves.model.enums.AuthorRole;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/// Maps raw RanobeDB API JSON responses to OpenShelves domain objects.
///
/// RanobeDB's `/book/{id}` response nests most useful data under locale-tagged arrays
/// (`titles`, `releases`, `publishers`) rather than flat fields, since a light novel
/// frequently has both an original-language and an official English release. This mapper
/// prefers the English (`"en"`) entry in each array, falling back to the book's default
/// fields when no English entry exists.
///
/// @see ExternalBook
@Component
public class RanobeDbMapper {

    /// Maps a RanobeDB `book` object (the `.book` field of a `/book/{id}` response) to an
    /// [ExternalBook].
    ///
    /// @param book the JSON object at `data.book` (or equivalent) from a book-detail response
    /// @return the normalized [ExternalBook]
    public ExternalBook toExternalBook(JsonNode book) {
        ExternalBook.ExternalBookBuilder builder = ExternalBook.builder();

        JsonNode englishTitle = firstMatching(book.get("titles"), "lang", "en", "official", true);
        JsonNode englishRelease = firstMatching(book.get("releases"), "lang", "en", null, false);
        JsonNode englishPublisher = firstPublisher(book.get("publishers"));
        JsonNode series = book.get("series");

        builder.title(englishTitle != null ? text(englishTitle, "title") : text(book, "title"));
        builder.description(text(book, "description"));
        builder.publisher(englishPublisher != null ? text(englishPublisher, "name") : null);
        builder.language(englishRelease != null ? text(englishRelease, "lang") : text(book, "lang"));
        builder.coverImageUrl(extractImageUrl(book.get("image")));
        builder.authors(extractAuthors(book.get("editions")));

        if (series != null) {
            builder.seriesName(text(series, "title"));
            builder.seriesNumber(findSeriesPosition(series, text(book, "id")));
        }

        Long releaseDate = englishRelease != null ? longValue(englishRelease, "release_date") : longValue(book, "c_release_date");
        builder.publicationYear(extractYearFromCompactDate(releaseDate));

        return builder.build();
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asString();
    }

    private Long longValue(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asLong();
    }

    // Finds the first array entry matching field == value, optionally requiring a boolean
    // flag (e.g. "official") to also be true. Falls back to the first entry if no locale
    // match exists but requireFlagIfPresent is false and the array is non-empty.
    private JsonNode firstMatching(JsonNode array, String field, String value, String flagField, boolean requireFlag) {
        if (array == null || !array.isArray()) {
            return null;
        }

        for (JsonNode entry : array) {
            String fieldValue = text(entry, field);
            if (value.equalsIgnoreCase(fieldValue)) {
                if (!requireFlag || flagField == null || (entry.has(flagField) && entry.get(flagField).asBoolean(false))) {
                    return entry;
                }
            }
        }
        return null;
    }

    private JsonNode firstPublisher(JsonNode publishers) {
        if (publishers == null || !publishers.isArray()) {
            return null;
        }

        for (JsonNode publisher : publishers) {
            if ("en".equalsIgnoreCase(text(publisher, "lang"))
                && "PUBLISHER".equalsIgnoreCase(text(publisher, "publisher_type"))) {
                return publisher;
            }
        }
        return null;
    }

    // Base URL for RanobeDB's image CDN; cover filenames are relative to this.
    private static final String RANOBEDB_IMAGE_BASE_URL = "https://images.ranobedb.org/";

    private String extractImageUrl(JsonNode imageNode) {
        String filename = text(imageNode, "filename");
        return filename != null ? RANOBEDB_IMAGE_BASE_URL + filename : null;
    }

    // Determines this book's 1-based position within its series by locating its own id in
    // the series' book list.
    private Integer findSeriesPosition(JsonNode series, String bookId) {
        JsonNode seriesBooks = series.get("books");
        if (seriesBooks == null || !seriesBooks.isArray() || bookId == null) {
            return null;
        }

        int index = 0;
        for (JsonNode seriesBook : seriesBooks) {
            if (bookId.equals(text(seriesBook, "id"))) {
                return index + 1;
            }
            index++;
        }
        return null;
    }

    // RanobeDB encodes dates as an integer in YYYYMMDD form (e.g. 20230514); a value of
    // null or 0 means "unknown".
    private Short extractYearFromCompactDate(Long compactDate) {
        if (compactDate == null || compactDate == 0) {
            return null;
        }
        return (short) (compactDate / 10000);
    }

    // Extracts author credits (role AUTHOR) from the editions[].staff[] arrays, preferring
    // the romanized name when available.
    private List<ExternalBook.AuthorRef> extractAuthors(JsonNode editions) {
        List<ExternalBook.AuthorRef> refs = new ArrayList<>();
        if (editions == null || !editions.isArray()) {
            return refs;
        }

        for (JsonNode edition : editions) {
            JsonNode staff = edition.get("staff");
            if (staff == null || !staff.isArray()) continue;

            for (JsonNode member : staff) {
                if (!"AUTHOR".equalsIgnoreCase(text(member, "role_type"))) continue;

                String name = StringUtils.isNotBlank(text(member, "romaji"))
                    ? text(member, "romaji")
                    : text(member, "name");
                if (name == null) continue;

                refs.add(ExternalBook.AuthorRef.builder()
                    .name(name)
                    .role(AuthorRole.AUTHOR)
                    .build());
            }
        }
        return refs;
    }
}
