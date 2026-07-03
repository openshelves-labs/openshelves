package com.openshelves.services.metadata.client.comicvine;

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

/// Maps raw Comic Vine API JSON responses to OpenShelves domain objects.
///
/// Comic Vine's `/search/` endpoint returns a mix of resource types (`volume`, `issue`,
/// `person`, ...) in one flat array, distinguished by a `resource_type` field. This mapper
/// only ever handles `volume` and `issue` results for [#issueToExternalBook]/
/// [#volumeToExternalBook] — callers are expected to have already filtered the result set.
///
/// Comics rarely carry ISBNs; `isbn10`/`isbn13` are intentionally left unset here, since
/// Comic Vine does not expose them.
///
/// @see ExternalBook
/// @see ExternalAuthor
@Component
public class ComicVineMapper {

    /// Matches a leading 4-digit year in a `start_year`/`cover_date` value.
    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})");

    /// Maps a Comic Vine `issue` resource to an [ExternalBook].
    ///
    /// The issue's parent volume supplies the series name/publisher context, since an issue
    /// on its own carries neither.
    ///
    /// @param issue the JSON object for a single `issue` resource
    /// @return the normalized [ExternalBook]
    public ExternalBook issueToExternalBook(JsonNode issue) {
        ExternalBook.ExternalBookBuilder builder = ExternalBook.builder();

        JsonNode volume = issue.get("volume");
        String volumeName = text(volume, "name");
        String issueNumber = text(issue, "issue_number");
        String issueName = text(issue, "name");

        builder.title(formatIssueTitle(volumeName, issueNumber, issueName));
        builder.description(firstNonBlank(text(issue, "description"), text(issue, "deck")));
        builder.seriesName(volumeName);
        builder.seriesNumber(parseIssueNumber(issueNumber));

        String date = firstNonBlank(text(issue, "store_date"), text(issue, "cover_date"));
        builder.publicationYear(extractYear(date));

        builder.coverImageUrl(extractImageUrl(issue.get("image")));
        builder.authors(extractWriters(issue.get("person_credits")));

        return builder.build();
    }

    /// Maps a Comic Vine `volume` resource to an [ExternalBook] representing the series as a
    /// whole (used when a search matches the volume itself rather than a specific issue).
    ///
    /// @param volume the JSON object for a single `volume` resource
    /// @return the normalized [ExternalBook]
    public ExternalBook volumeToExternalBook(JsonNode volume) {
        ExternalBook.ExternalBookBuilder builder = ExternalBook.builder();

        String name = text(volume, "name");
        builder.title(name);
        builder.seriesName(name);
        builder.description(firstNonBlank(text(volume, "description"), text(volume, "deck")));
        builder.publisher(text(volume, "publisher", "name"));
        builder.publicationYear(extractYear(text(volume, "start_year")));
        builder.coverImageUrl(extractImageUrl(volume.get("image")));

        return builder.build();
    }

    /// Maps a Comic Vine `person` search resource to an [ExternalAuthor].
    ///
    /// Comic Vine has no dedicated "author" concept — contributors of all kinds (writers,
    /// artists, letterers, ...) live in a single `person` resource type.
    ///
    /// @param person the JSON object for a single `person` resource
    /// @return the normalized [ExternalAuthor]
    public ExternalAuthor toExternalAuthor(JsonNode person) {
        ExternalAuthor.ExternalAuthorBuilder builder = ExternalAuthor.builder();

        builder.name(text(person, "name"));
        builder.bio(firstNonBlank(text(person, "description"), text(person, "deck")));
        builder.profileImageUrl(extractImageUrl(person.get("image")));

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

    private String text(JsonNode node, String field, String nestedField) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return text(node.get(field), nestedField);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) return value;
        }
        return null;
    }

    private String extractImageUrl(JsonNode imageNode) {
        // Prefer medium_url: full-size covers are large enough to be wasteful for catalog thumbnails.
        String url = text(imageNode, "medium_url");
        return url != null ? url : text(imageNode, "small_url");
    }

    private Short extractYear(String value) {
        if (StringUtils.isBlank(value)) return null;

        Matcher matcher = YEAR_PATTERN.matcher(value);
        if (matcher.find()) {
            return Short.parseShort(matcher.group(1));
        }
        return null;
    }

    private Integer parseIssueNumber(String issueNumber) {
        if (StringUtils.isBlank(issueNumber)) return null;
        try {
            return (int) Double.parseDouble(issueNumber);
        } catch (NumberFormatException e) {
            return null;
        }
    }

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
    private List<ExternalBook.AuthorRef> extractWriters(JsonNode personCredits) {
        List<ExternalBook.AuthorRef> refs = new ArrayList<>();
        if (personCredits == null || !personCredits.isArray()) {
            return refs;
        }

        for (JsonNode credit : personCredits) {
            String role = text(credit, "role");
            String name = text(credit, "name");
            if (name == null || role == null) continue;

            String lowerRole = role.toLowerCase();
            if (lowerRole.contains("writer") || lowerRole.contains("script") || lowerRole.contains("story")) {
                refs.add(ExternalBook.AuthorRef.builder()
                    .name(name)
                    .role(AuthorRole.AUTHOR)
                    .build());
            }
        }
        return refs;
    }
}
