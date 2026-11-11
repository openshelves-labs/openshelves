package com.openshelves.metadata.client.impl.hardcover;

import com.openshelves.metadata.enums.AuthorRole;
import com.openshelves.metadata.model.AuthorMetadata;
import com.openshelves.metadata.model.BookMetadata;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Maps raw Hardcover GraphQL API JSON responses to OpenShelves domain objects.
///
/// Hardcover exposes a conventional Hasura-style GraphQL schema over Postgres, so unlike
/// Open Library or Google Books, most fields arrive already well-typed. The main source of
/// impedance is `cached_contributors`, a denormalized JSON array Hardcover stores directly
/// on the book row (`[{"author": {"name": "..."}, "contribution": "..."}]`) rather than a
/// proper relation — this mapper unpacks it into [BookMetadata.LinkedAuthor]s.
///
/// @see BookMetadata
/// @see AuthorMetadata
@Component
public class HardcoverMapper {

    /// Matches a leading 4-digit year in a `release_date` value such as `"2003-05-14"`.
    private static final Pattern YEAR_PATTERN = Pattern.compile("^(\\d{4})");


    // -----------------------------------------------------------------------
    // Public Mapper API
    // -----------------------------------------------------------------------

    /// Maps a single `books` query result node to an [BookMetadata].
    ///
    /// @param bookNode the JSON object for one entry in the `books` GraphQL result array
    /// @return the normalized [BookMetadata]
    public BookMetadata convertToBookMetadata(JsonNode bookNode) {
        var bookBuilder = BookMetadata.builder();

        bookBuilder.withTitle(text(bookNode, "title"));
        bookBuilder.withSubtitle(text(bookNode, "subtitle"));
        bookBuilder.withDescription(text(bookNode, "description"));
        bookBuilder.withPublicationYear(extractYear(text(bookNode, "release_date")));

        if (bookNode.has("pages") && !bookNode.get("pages").isNull()) {
            bookBuilder.withPageCount(bookNode.get("pages").asInt());
        }

        bookBuilder.withCoverImageUrl(extractImageUrl(bookNode.get("image")));
        bookBuilder.withAuthors(extractLinkedAuthors(bookNode.get("cached_contributors")));

        // The primary edition (if requested in the query) carries publisher/language/ISBNs,
        // since those are properties of a specific printing rather than the abstract work.
        JsonNode primaryEdition = firstEdition(bookNode.get("editions"));
        if (primaryEdition != null) {
            bookBuilder.withIsbn10(text(primaryEdition, "isbn_10"));
            bookBuilder.withIsbn13(text(primaryEdition, "isbn_13"));
            bookBuilder.withLanguage(text(primaryEdition, "language", "code2"));
            bookBuilder.withPublisher(text(primaryEdition, "publisher", "name"));
        }

        return bookBuilder.build();
    }

    /// Maps a single `authors` query result node to an [AuthorMetadata].
    ///
    /// @param authorNode the JSON object for one entry in the `authors` GraphQL result array
    /// @return the normalized [AuthorMetadata]
    public AuthorMetadata convertToAuthorMetadata(JsonNode authorNode) {

        return AuthorMetadata.builder()
            .withName(text(authorNode, "name"))
            .withBio(text(authorNode, "bio"))
            .withBirthYear(extractYear(text(authorNode, "born_date")))
            .withDeathYear(extractYear(text(authorNode, "death_date")))
            .withProfileImageUrl(extractImageUrl(authorNode.get("image")))
            .build();
    }


    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    /// Safely extracts a text field from a JSON node, returning `null` if the node,
    /// field, or field value is `null`.
    ///
    /// @param node the JSON node to read; may be `null`
    /// @param field the name of the field to extract
    /// @return the field's text value, or `null` if unavailable
    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asString();
    }

    /// Safely extracts a text field nested within an object.
    ///
    /// @param node the JSON node to read; may be `null`
    /// @param field the name of the parent field
    /// @param nestedField the name of the nested field to extract
    /// @return the nested field's text value, or `null` if unavailable
    private String text(JsonNode node, String field, String nestedField) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return text(node.get(field), nestedField);
    }

    /// Extracts the leading year from a date value.
    ///
    /// @param date the date value; may be blank
    /// @return the year, or `null` if the value does not start with a year
    private Short extractYear(String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }

        Matcher matcher = YEAR_PATTERN.matcher(date);
        if (matcher.find()) {
            return Short.parseShort(matcher.group(1));
        }
        return null;
    }

    /// Returns the URL from a Hardcover image object.
    ///
    /// @param imageNode the image object; may be `null`
    /// @return the image URL, or `null` if it is unavailable
    private String extractImageUrl(JsonNode imageNode) {
        return text(imageNode, "url");
    }

    /// Returns the first edition in an editions array.
    ///
    /// @param editions the editions array; may be `null`
    /// @return the first edition, or `null` if the array is absent or empty
    private JsonNode firstEdition(JsonNode editions) {
        if (editions == null || !editions.isArray() || editions.isEmpty()) {
            return null;
        }
        return editions.get(0);
    }

    /// Maps Hardcover's `cached_contributors` entries to linked authors.
    ///
    /// Entries without a contribution are assigned the author role; entries with a
    /// contribution are assigned the contributor role.
    ///
    /// @param cachedContributors the book's contributor entries; may be `null`
    /// @return the linked authors found in the entries; may be empty
    private List<BookMetadata.LinkedAuthor> extractLinkedAuthors(JsonNode cachedContributors) {
        List<BookMetadata.LinkedAuthor> linkedAuthors = new ArrayList<>();
        if (cachedContributors == null || !cachedContributors.isArray()) {
            return linkedAuthors;
        }

        for (JsonNode contributor : cachedContributors) {
            String name = text(contributor, "author", "name");
            if (name == null) continue;

            String contribution = text(contributor, "contribution");

            linkedAuthors.add(
                BookMetadata.LinkedAuthor.builder()
                    .withName(name)
                    .withRole(StringUtils.isBlank(contribution) ? AuthorRole.AUTHOR : AuthorRole.CONTRIBUTOR)
                    .build());
        }
        return linkedAuthors;
    }
}
