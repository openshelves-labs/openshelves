package com.openshelves.metadata.client.impl.comicvine;

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

/// Maps raw Comic Vine API JSON responses to OpenShelves domain objects.
///
/// Comic Vine's `/search/` endpoint returns a mix of resource types (`volume`, `issue`,
/// `person`, ...) in one flat array, distinguished by a `resource_type` field. This mapper
/// only ever handles `volume` and `issue` results — callers are expected to have already filtered the result set.
///
/// Comics rarely carry ISBNs; `isbn10`/`isbn13` are intentionally left unset here, since
/// Comic Vine does not expose them.
///
/// @see BookMetadata
/// @see AuthorMetadata
@Component
public class ComicVineMapper {

    /// Matches a leading 4-digit year in a `start_year`/`cover_date` value.
    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})");


    // -----------------------------------------------------------------------
    // Public Mapper API
    // -----------------------------------------------------------------------

    /// Maps a Comic Vine `issue` resource to an [BookMetadata].
    ///
    /// The issue's parent volume supplies the series name/publisher context, since an issue
    /// on its own carries neither.
    ///
    /// @param issueNode the JSON object for a single `issue` resource
    /// @return the normalized [BookMetadata]
    public BookMetadata convertIssueToBookMetadata(JsonNode issueNode) {
        JsonNode volumeNode = issueNode.get("volume");
        String volumeName = text(volumeNode, "name");

        String issueNumber = text(issueNode, "issue_number");
        String issueName = text(issueNode, "name");

        String publicationDate = firstNonBlank(text(issueNode, "store_date"), text(issueNode, "cover_date"));

        return BookMetadata.builder()
            .withTitle(formatIssueTitle(volumeName, issueNumber, issueName))
            .withDescription(firstNonBlank(text(issueNode, "description"), text(issueNode, "deck")))
            .withSeriesName(volumeName)
            .withSeriesNumber(parseIssueNumber(issueNumber))
            .withPublicationYear(extractYear(publicationDate))
            .withCoverImageUrl(extractImageUrl(issueNode.get("image")))
            .withAuthors(extractWriters(issueNode.get("person_credits")))
            .build();
    }

    /// Maps a Comic Vine `volume` resource to an [BookMetadata] representing the series as a
    /// whole (used when a search matches the volume itself rather than a specific issue).
    ///
    /// @param volumeNode the JSON object for a single `volume` resource
    /// @return the normalized [BookMetadata]
    public BookMetadata convertVolumeToBookMetadata(JsonNode volumeNode) {
        String volumeName = text(volumeNode, "name");

        return BookMetadata.builder()
            .withTitle(volumeName)
            .withSeriesName(volumeName)
            .withDescription(firstNonBlank(text(volumeNode, "description"), text(volumeNode, "deck")))
            .withPublisher(text(volumeNode, "publisher", "name"))
            .withPublicationYear(extractYear(text(volumeNode, "start_year")))
            .withCoverImageUrl(extractImageUrl(volumeNode.get("image")))
            .build();
    }

    /// Maps a Comic Vine `person` search resource to an [AuthorMetadata].
    ///
    /// Comic Vine has no dedicated "author" concept — contributors of all kinds (writers,
    /// artists, letterers, ...) live in a single `person` resource type.
    ///
    /// @param authorNode the JSON object for a single `person` resource
    /// @return the normalized [AuthorMetadata]
    public AuthorMetadata convertToAuthorMetadata(JsonNode authorNode) {

        return AuthorMetadata.builder()
            .withName(text(authorNode, "name"))
            .withBio(firstNonBlank(text(authorNode, "description"), text(authorNode, "deck")))
            .withProfileImageUrl(extractImageUrl(authorNode.get("image")))
            .build();
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

    // Safely extracts a nested text field from a node, returning null if absent or JSON-null.
    private String text(JsonNode node, String field, String nestedField) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return text(node.get(field), nestedField);
    }

    // Returns the first non-blank value from a list of strings, or null if all are blank.
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) return value;
        }
        return null;
    }

    // Extracts the best available cover image URL from a Comic Vine volume or issue node.
    private String extractImageUrl(JsonNode imageNode) {
        // Prefer medium_url: full-size covers are large enough to be wasteful for catalog thumbnails.
        String url = text(imageNode, "medium_url");
        return url != null ? url : text(imageNode, "small_url");
    }

    // Extracts a 4-digit year from a Comic Vine `start_year` or `cover_date` string.
    private Short extractYear(String value) {
        if (StringUtils.isBlank(value)) return null;

        Matcher matcher = YEAR_PATTERN.matcher(value);
        if (matcher.find()) {
            return Short.parseShort(matcher.group(1));
        }
        return null;
    }

    // Parses a Comic Vine issue number string into an integer, returning null if the string is blank or not a valid number.
    private Integer parseIssueNumber(String issueNumber) {
        if (StringUtils.isBlank(issueNumber)) return null;
        try {
            return (int) Double.parseDouble(issueNumber);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Formats a Comic Vine issue title as "Volume Name #Issue Number - Issue Name", omitting any blank components.
    private String formatIssueTitle(String volumeName, String issueNumber, String issueName) {
        if (volumeName == null) {
            return issueName;
        }

        String title = volumeName + (StringUtils.isNotBlank(issueNumber) ? " #" + issueNumber : "");
        if (StringUtils.isNotBlank(issueName) && !issueName.equalsIgnoreCase(volumeName)) {
            title += " - " + issueName;
        }
        return title;
    }

    // Extracts writer credits from person_credits, matching on role fragments (Comic Vine
    // stores roles as a free-text comma-separated string, e.g. "writer, plot").
    private List<BookMetadata.LinkedAuthor> extractWriters(JsonNode personCredits) {
        List<BookMetadata.LinkedAuthor> linkedAuthors = new ArrayList<>();
        if (personCredits == null || !personCredits.isArray()) {
            return linkedAuthors;
        }

        for (JsonNode credit : personCredits) {
            String role = text(credit, "role");
            String name = text(credit, "name");

            if (name == null || role == null) continue;

            String lowerRole = role.toLowerCase();
            if (lowerRole.contains("writer") || lowerRole.contains("script") || lowerRole.contains("story")) {

                linkedAuthors.add(
                    BookMetadata.LinkedAuthor.builder()
                        .withName(name)
                        .withRole(AuthorRole.AUTHOR)
                        .build());
            }
        }

        return linkedAuthors;
    }
}
