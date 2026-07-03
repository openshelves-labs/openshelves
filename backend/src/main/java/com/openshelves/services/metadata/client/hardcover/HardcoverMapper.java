package com.openshelves.services.metadata.client.hardcover;

import com.openshelves.model.dto.metadata.ExternalAuthor;
import com.openshelves.model.dto.metadata.ExternalBook;
import com.openshelves.model.enums.AuthorRole;
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
/// proper relation — this mapper unpacks it into [ExternalBook.AuthorRef]s.
///
/// @see ExternalBook
/// @see ExternalAuthor
@Component
public class HardcoverMapper {

    /// Matches a leading 4-digit year in a `release_date` value such as `"2003-05-14"`.
    private static final Pattern YEAR_PATTERN = Pattern.compile("^(\\d{4})");

    /// Maps a single `books` query result node to an [ExternalBook].
    ///
    /// @param book the JSON object for one entry in the `books` GraphQL result array
    /// @return the normalized [ExternalBook]
    public ExternalBook toExternalBook(JsonNode book) {
        ExternalBook.ExternalBookBuilder builder = ExternalBook.builder();

        builder.title(text(book, "title"));
        builder.subtitle(text(book, "subtitle"));
        builder.description(text(book, "description"));
        builder.publicationYear(extractYear(text(book, "release_date")));

        if (book.has("pages") && !book.get("pages").isNull()) {
            builder.pageCount(book.get("pages").asInt());
        }

        builder.coverImageUrl(extractImageUrl(book.get("image")));
        builder.authors(extractAuthorRefs(book.get("cached_contributors")));

        // The primary edition (if requested in the query) carries publisher/language/ISBNs,
        // since those are properties of a specific printing rather than the abstract work.
        JsonNode primaryEdition = firstEdition(book.get("editions"));
        if (primaryEdition != null) {
            builder.isbn10(text(primaryEdition, "isbn_10"));
            builder.isbn13(text(primaryEdition, "isbn_13"));
            builder.language(text(primaryEdition, "language", "code2"));
            builder.publisher(text(primaryEdition, "publisher", "name"));
        }

        return builder.build();
    }

    /// Maps a single `authors` query result node to an [ExternalAuthor].
    ///
    /// @param author the JSON object for one entry in the `authors` GraphQL result array
    /// @return the normalized [ExternalAuthor]
    public ExternalAuthor toExternalAuthor(JsonNode author) {
        ExternalAuthor.ExternalAuthorBuilder builder = ExternalAuthor.builder();

        builder.name(text(author, "name"));
        builder.bio(text(author, "bio"));
        builder.birthYear(extractYear(text(author, "born_date")));
        builder.deathYear(extractYear(text(author, "death_date")));
        builder.profileImageUrl(extractImageUrl(author.get("image")));

        return builder.build();
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

    // Safely extracts a text field from a nested object (e.g. edition.publisher.name).
    private String text(JsonNode node, String field, String nestedField) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return text(node.get(field), nestedField);
    }

    // Parses the leading 4-digit year out of a loosely formatted date string.
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

    // Extracts the "url" field from an {"url": "..."} image object.
    private String extractImageUrl(JsonNode imageNode) {
        return text(imageNode, "url");
    }

    // Returns the first entry of an "editions" array, or null if absent/empty.
    private JsonNode firstEdition(JsonNode editions) {
        if (editions == null || !editions.isArray() || editions.isEmpty()) {
            return null;
        }
        return editions.get(0);
    }

    // Unpacks Hardcover's denormalized "cached_contributors" JSON array into AuthorRefs.
    // Shape: [{"author": {"name": "..."}, "contribution": "..." | null}]. A null/blank
    // "contribution" denotes the primary author; anything else is treated as a contributor.
    private List<ExternalBook.AuthorRef> extractAuthorRefs(JsonNode cachedContributors) {
        List<ExternalBook.AuthorRef> refs = new ArrayList<>();
        if (cachedContributors == null || !cachedContributors.isArray()) {
            return refs;
        }

        for (JsonNode contributor : cachedContributors) {
            String name = text(contributor, "author", "name");
            if (name == null) continue;

            String contribution = text(contributor, "contribution");
            refs.add(ExternalBook.AuthorRef.builder()
                .name(name)
                .role(StringUtils.isBlank(contribution) ? AuthorRole.AUTHOR : AuthorRole.CONTRIBUTOR)
                .build());
        }
        return refs;
    }
}
