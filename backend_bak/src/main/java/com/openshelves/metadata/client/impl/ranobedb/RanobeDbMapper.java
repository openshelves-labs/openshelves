package com.openshelves.metadata.client.impl.ranobedb;

import com.openshelves.metadata.model.BookMetadata;
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
/// @see BookMetadata
@Component
public class RanobeDbMapper {

    // Base URL for RanobeDB's image CDN; cover filenames are relative to this.
    private static final String RANOBEDB_IMAGE_BASE_URL = "https://images.ranobedb.org/";


    // -----------------------------------------------------------------------
    // Public Mapper API
    // -----------------------------------------------------------------------

    /// Maps a RanobeDB `book` object (the `.book` field of a `/book/{id}` response) to an [BookMetadata].
    ///
    /// @param bookNode the JSON object at `data.book` (or equivalent) from a book-detail response
    /// @return the normalized [BookMetadata]
    public BookMetadata convertToBookMetadata(JsonNode bookNode) {
        var bookBuilder = BookMetadata.builder();

        JsonNode englishTitle = firstMatching(bookNode.get("titles"), "lang", "en", "official", true);
        JsonNode englishRelease = firstMatching(bookNode.get("releases"), "lang", "en", null, false);
        JsonNode englishPublisher = firstPublisher(bookNode.get("publishers"));
        JsonNode series = bookNode.get("series");

        bookBuilder.withTitle(englishTitle != null ? text(englishTitle, "title") : text(bookNode, "title"));
        bookBuilder.withDescription(text(bookNode, "description"));
        bookBuilder.withPublisher(englishPublisher != null ? text(englishPublisher, "name") : null);
        bookBuilder.withLanguage(englishRelease != null ? text(englishRelease, "lang") : text(bookNode, "lang"));
        bookBuilder.withCoverImageUrl(extractImageUrl(bookNode.get("image")));
        bookBuilder.withAuthors(extractLinkedAuthors(bookNode.get("editions")));

        if (series != null) {
            bookBuilder.withSeriesName(text(series, "title"));
            bookBuilder.withSeriesNumber(findSeriesPosition(series, text(bookNode, "id")));
        }

        Long releaseDate = englishRelease != null ? longValue(englishRelease, "release_date") : longValue(bookNode, "c_release_date");
        bookBuilder.withPublicationYear(extractYearFromCompactDate(releaseDate));

        return bookBuilder.build();
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    /// Safely extracts a text field from a JSON node, returning `null` if the node
    /// is `null`, the field is absent, or the field is JSON-null.
    ///
    /// @param node the JSON node to read; may be `null`
    /// @param field the name of the field to extract
    /// @return the field's text value, or `null` if it is unavailable
    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asString();
    }

    /// Safely extracts a numeric field from a JSON node.
    ///
    /// @param node the JSON node to read; may be `null`
    /// @param field the name of the field to extract
    /// @return the field's value, or `null` if the node, field, or value is `null`
    private Long longValue(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asLong();
    }

    /// Finds the first entry in an array whose field matches the requested value.
    ///
    /// When `requireFlag` is `true` and `flagField` is provided, the entry must
    /// also have that field set to `true`.
    ///
    /// @param array the array of entries to search; may be `null`
    /// @param field the field to compare against `value`
    /// @param value the value to match, ignoring case
    /// @param flagField the optional boolean field to require
    /// @param requireFlag whether a matching entry must have `flagField` set to `true`
    /// @return the first matching entry, or `null` if none matches
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

    /// Finds the first English publisher entry in a publisher array.
    ///
    /// @param publishers the publisher entries; may be `null`
    /// @return the first English entry with publisher type `PUBLISHER`, or `null` if none matches
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

    /// Builds the image URL from a RanobeDB image object's filename.
    ///
    /// @param imageNode the image object; may be `null`
    /// @return the image URL, or `null` if no filename is available
    private String extractImageUrl(JsonNode imageNode) {
        String filename = text(imageNode, "filename");
        return filename != null ? RANOBEDB_IMAGE_BASE_URL + filename : null;
    }

    /// Finds a book's 1-based position in its series.
    ///
    /// @param series the series object containing its books
    /// @param bookId the RanobeDB identifier of the book
    /// @return the book's position, or `null` if it is not listed in the series
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

    /// Extracts the year from a RanobeDB compact date value in `YYYYMMDD` form.
    ///
    /// @param compactDate the compact date; `null` or `0` represents an unknown date
    /// @return the year, or `null` if the date is unknown
    private Short extractYearFromCompactDate(Long compactDate) {
        if (compactDate == null || compactDate == 0) {
            return null;
        }
        return (short) (compactDate / 10000);
    }

    /// Extracts author credits from the staff lists of a book's editions.
    ///
    /// Uses the romanized name when present, otherwise the staff member's name.
    ///
    /// @param editions the book's edition array; may be `null`
    /// @return the linked authors found in the editions; may be empty
    private List<BookMetadata.LinkedAuthor> extractLinkedAuthors(JsonNode editions) {
        List<BookMetadata.LinkedAuthor> linkedAuthors = new ArrayList<>();
        if (editions == null || !editions.isArray()) {
            return linkedAuthors;
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

                linkedAuthors.add(
                    BookMetadata.LinkedAuthor.builder()
                        .withName(name)
                        .build());
            }
        }
        return linkedAuthors;
    }
}
