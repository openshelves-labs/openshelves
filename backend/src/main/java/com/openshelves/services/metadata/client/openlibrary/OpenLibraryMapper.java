package com.openshelves.services.metadata.client.openlibrary;

import com.openshelves.model.dto.metadata.ExternalAuthor;
import com.openshelves.model.dto.metadata.ExternalBook;
import com.openshelves.model.enums.AuthorRole;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Maps raw Open Library API JSON responses to OpenShelves domain objects.
///
/// The Open Library API returns data in a loosely structured JSON format where
/// fields may be plain strings, typed value objects (`{"type": "...", "value":
/// "..."}`), arrays, or reference objects (`{"key": "/works/OL123W"}`). This
/// mapper normalises all such variations into clean [ExternalBook] and
/// [ExternalAuthor] DTOs that the rest of the application can consume without
/// being coupled to the API's schema.
///
/// **Key responsibilities:**
/// - Extracting and normalising text fields (plain strings and typed value
///   objects).
/// - Resolving Open Library key references (e.g. `/works/OL123W`) to bare OLIDs.
/// - Translating MARC ISO-639-2/B language codes to ISO-639-1 two-letter codes.
/// - Parsing free-form series strings into a (name, number) pair.
/// - Building cover and author photo URLs from numeric cover/photo IDs.
///
/// @see ExternalBook
/// @see ExternalAuthor
@Component
public class OpenLibraryMapper {

    /// URL template for book cover images served by the Open Library Covers API.
    /// The `%d` placeholder is replaced with the numeric cover ID found in the
    /// `covers` array of an edition document. The `-L` suffix requests the
    /// large-size image.
    private static final String BOOK_COVER_URL = "https://covers.openlibrary.org/b/id/%d-L.jpg";

    /// URL template for author portrait images served by the Open Library Covers
    /// API. The `%d` placeholder is replaced with the numeric photo ID found in the
    /// `photos` array of an author document. The `-L` suffix requests the
    /// large-size image.
    private static final String AUTHOR_PHOTO_URL = "https://covers.openlibrary.org/a/id/%d-L.jpg";

    /// Regex that matches a standalone 4-digit year anywhere within a date string.
    /// Used to extract a publication, birth, or death year from loosely formatted
    /// dates such as `"January 1, 2003"`, `"2003"`, or `"ca. 1850"`.
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(\\d{4})\\b");

    /// Matches a trailing series number: `#1`, `Vol. 3`, `Book 2`, `Part 1`,
    /// `(27)`, etc.
    ///
    /// The pattern captures the numeric portion in one of three groups:
    ///
    ///     1. Group 1 – keyword-prefixed number, e.g. `Vol. 3` → `3`
    ///     2. Group 2 – hash-prefixed number, e.g. `#1` → `1`
    ///     3. Group 3 – parenthesised number, e.g. `(27)` → `27`
    private static final Pattern SERIES_NUMBER_PATTERN = Pattern.compile(
            "[,\\s]++(?:no|vol|book|part|volume)\\.?\\s*+(\\d++)|#(\\d++)|\\.?(\\d++)\\)",
            Pattern.CASE_INSENSITIVE);

    /// Mapping from MARC ISO-639-2/B (and select ISO-639-2/T alias) language codes
    /// to ISO-639-1 two-letter codes.
    ///
    /// Open Library edition documents store language references as
    /// `/languages/eng`-style keys; the trailing code segment is looked up in this
    /// map to produce a standardised two-letter code that can be stored and
    /// displayed uniformly across the platform.
    ///
    /// Dual entries are included for codes that have both a bibliographic (B) and
    /// terminological (T) form in the ISO-639-2 standard, e.g.:
    /// - `ces` / `cze` → `cs` (Czech)
    /// - `rum` / `ron` → `ro` (Romanian)
    private static final Map<String, String> MARC_TO_ISO_639_1 = Map.ofEntries(
            Map.entry("eng", "en"), Map.entry("fre", "fr"), Map.entry("ger", "de"),
            Map.entry("spa", "es"), Map.entry("ita", "it"), Map.entry("por", "pt"),
            Map.entry("rus", "ru"), Map.entry("chi", "zh"), Map.entry("jpn", "ja"),
            Map.entry("ara", "ar"), Map.entry("hin", "hi"), Map.entry("dut", "nl"),
            Map.entry("swe", "sv"), Map.entry("nor", "no"), Map.entry("dan", "da"),
            Map.entry("fin", "fi"), Map.entry("pol", "pl"), Map.entry("tur", "tr"),
            Map.entry("kor", "ko"), Map.entry("vie", "vi"), Map.entry("ind", "id"),
            Map.entry("lat", "la"), Map.entry("gre", "el"), Map.entry("heb", "he"),
            Map.entry("ces", "cs"), Map.entry("cze", "cs"), Map.entry("hun", "hu"),
            Map.entry("rum", "ro"), Map.entry("ron", "ro"), Map.entry("ukr", "uk"),
            Map.entry("cat", "ca"), Map.entry("hrv", "hr"), Map.entry("slk", "sk"),
            Map.entry("bul", "bg"), Map.entry("srp", "sr"), Map.entry("slv", "sl"));

    // -----------------------------------------------------------------------
    // Public Mapper API
    // -----------------------------------------------------------------------

    /// Converts a raw Open Library *edition* JSON document into an
    /// [ExternalBook] DTO.
    ///
    /// The method is tolerant of missing or null fields; any field that cannot be
    /// resolved from the JSON is left as `null` on the resulting object.
    ///
    /// @param node the root [JsonNode] of an Open Library edition response; may be
    ///             `null`
    /// @return a populated [ExternalBook], or `null` if `node` is `null` or a
    ///         JSON null
    public ExternalBook toExternalBook(JsonNode node) {
        if (node == null || node.isNull())
            return null;

        // Parse the series field once; result is [seriesName, seriesNumber] or [null,
        // null]
        String[] seriesInfo = parseSeries(firstInArray(node, "series"));

        // Resolve the first positive cover ID for building the cover image URL
        Long coverId = extractFirstPositiveNumberInArray(node, "covers");

        return ExternalBook.builder()
                .title(text(node, "title"))
                .subtitle(text(node, "subtitle"))
                .description(text(node, "description"))
                .language(extractLanguageCode(node))
                .pageCount(integer(node, "number_of_pages"))
                .deweyDecimal(firstInArray(node, "dewey_decimal_class"))
                // lc_classifications is preferred; lc_classification is a legacy field on older
                // records
                .lcClassification(firstNonNull(
                        firstInArray(node, "lc_classifications"),
                        firstInArray(node, "lc_classification")))
                .publisher(firstInArray(node, "publishers"))
                .publicationYear(extractYearFromDate(text(node, "publish_date")))
                .seriesName(seriesInfo[0])
                .seriesNumber(seriesInfo[1] != null ? Integer.valueOf(seriesInfo[1]) : null)
                .isbn10(firstInArray(node, "isbn_10"))
                .isbn13(firstInArray(node, "isbn_13"))
                // ASIN may appear under "asin" or "amazon" within the identifiers sub-object
                .asin(firstNonNull(
                        firstInArray(node.get("identifiers"), "asin"),
                        firstInArray(node.get("identifiers"), "amazon")))
                .olid(extractOlidFromKey(text(node, "key")))
                .coverImageUrl(coverId != null ? String.format(BOOK_COVER_URL, coverId) : null)
                .authors(extractAuthorRefs(node))
                .build();
    }

    /// Converts a raw Open Library *author* JSON document into an
    /// [ExternalAuthor] DTO.
    ///
    /// The method is tolerant of missing or null fields; any field that cannot be
    /// resolved from the JSON is left as `null` on the resulting object.
    ///
    /// @param node the root [JsonNode] of an Open Library author response; may be
    ///             `null`
    /// @return a populated [ExternalAuthor], or `null` if `node` is `null` or a
    ///         JSON null
    public ExternalAuthor toExternalAuthor(JsonNode node) {
        if (node == null || node.isNull())
            return null;

        // Resolve the first positive photo ID for building the profile image URL
        Long photoId = extractFirstPositiveNumberInArray(node, "photos");

        return ExternalAuthor.builder()
                // personal_name is the more specific field; fall back to the generic name field
                .name(firstNonNull(
                        text(node, "personal_name"),
                        text(node, "name")))
                .bio(text(node, "bio"))
                .birthYear(extractYearFromDate(text(node, "birth_date")))
                .deathYear(extractYearFromDate(text(node, "death_date")))
                .olid(extractOlidFromKey(text(node, "key")))
                // Amazon ID is nested inside the remote_ids sub-object
                .asin(text(node.get("remote_ids"), "amazon"))
                .profileImageUrl(photoId != null ? String.format(AUTHOR_PHOTO_URL, photoId) : null)
                .build();
    }

    // -----------------------------------------------------------------------
    // OL-specific resolvers
    // -----------------------------------------------------------------------

    /// Extracts the bare Open Library identifier (OLID) from a full key path.
    ///
    /// Open Library uses slash-delimited key paths as identifiers, for example:
    /// - `/works/OL123W` → `OL123W`
    /// - `/authors/OL456A` → `OL456A`
    /// - `/books/OL789M` → `OL789M`
    ///
    /// If the key contains no slash, it is returned as-is under the assumption that
    /// it is already a bare OLID.
    ///
    /// @param key the full key path returned by the Open Library API; may be blank
    /// @return the bare OLID string, or `null` if `key` is blank
    private String extractOlidFromKey(String key) {
        if (StringUtils.isEmpty(key))
            return null;
        int idx = key.lastIndexOf('/');
        // If a slash is found, take everything after the last slash
        return idx > 0 ? key.substring(idx + 1) : key;
    }

    /// Parses a raw series string into a (name, number) pair.
    ///
    /// Open Library stores series information as a single free-form string such as
    /// `"The Lord of the Rings #1"` or `"Harry Potter, Vol. 3"`. This method splits
    /// that string into a clean series name and an optional series position number
    /// using [SERIES_NUMBER_PATTERN].
    ///
    /// Examples:
    /// ```
    /// "Discworld #27"          → ["Discworld", "27"]
    /// "Harry Potter, Vol. 3"   → ["Harry Potter", "3"]
    /// "The Dark Tower (7)"     → ["The Dark Tower", "7"]
    /// "A standalone series"    → ["A standalone series", null]
    /// null / ""                → [null, null]
    /// ```
    ///
    /// @param seriesEntry the raw series string from the Open Library response; may
    ///                    be `null` or empty
    /// @return a two-element `String[]` where index 0 is the series name and index 1
    ///         is the series number (both may be `null`)
    private String[] parseSeries(String seriesEntry) {
        if (StringUtils.isEmpty(seriesEntry))
            return new String[2];

        // Trim the input to remove leading/trailing whitespace before applying the
        // regex
        seriesEntry = seriesEntry.strip();

        Matcher m = SERIES_NUMBER_PATTERN.matcher(seriesEntry);
        if (m.find()) {
            // One of the three capture groups will hold the numeric part
            String seriesNumber = firstNonNull(m.group(1), m.group(2), m.group(3));
            String seriesName = seriesEntry.substring(0, m.start()).strip();
            if (seriesName.isEmpty()) {
                seriesName = seriesEntry; // Just a safe fallback
            }
            return new String[] { seriesName, seriesNumber };
        }

        // If no series number is found, return the original series name and null for
        // the number
        return new String[] { seriesEntry, null };
    }

    /// Extracts the first positive number from a JSON array field.
    ///
    /// Open Library uses integer arrays for cover and photo IDs; the value `-1` is
    /// used as a sentinel to indicate that no image is available. This method skips
    /// non-positive values to avoid constructing invalid URLs.
    ///
    /// @param node  the parent [JsonNode]; may be `null`
    /// @param field the name of the array field within `node`
    ///
    /// @return the first positive value found in the array as a [Long], or `null` if
    ///         the field is absent, not an array, empty, or contains no
    ///         positive numbers
    private Long extractFirstPositiveNumberInArray(JsonNode node, String field) {
        if (node == null || node.isNull())
            return null;

        JsonNode arrayNode = node.get(field);
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty())
            return null;

        for (JsonNode itemNode : arrayNode) {
            if (itemNode.isIntegralNumber() && itemNode.asLong() > 0) {
                return itemNode.asLong();
            }
        }

        return null;
    }

    /// Resolves the ISO-639-1 two-letter language code from a book's JSON node.
    ///
    /// Open Library may express a book's language in several ways:
    /// 1. A `languages` array of reference objects, e.g. `[{"key":
    ///    "/languages/eng"}]`
    /// 2. A `language` array (some older editions), with the same structure.
    /// 3. A plain `language_code` string field (fallback for non-standard records).
    ///
    /// The extracted code is looked up in [MARC_TO_ISO_639_1] and converted to a
    /// standard two-letter code. If no matching code is found, `null` is returned.
    ///
    /// @param node the root [JsonNode] of an Open Library edition; may be `null`
    /// @return an ISO-639-1 two-letter language code (e.g. `"en"`), or `null` if the
    ///         language cannot be determined or is not in the known mapping
    private String extractLanguageCode(JsonNode node) {
        if (node == null || node.isNull())
            return null;

        // Try the primary "languages" field first, then the legacy "language" field
        for (String fieldName : Arrays.asList("languages", "language")) {
            JsonNode langArr = node.get(fieldName);
            if (langArr == null || !langArr.isArray() || langArr.isEmpty())
                continue;

            JsonNode langRef = langArr.get(0);
            if (langRef == null || langRef.isNull())
                continue;

            // Language entries can be reference objects {"key": "/languages/eng"} or plain
            // strings
            String langKey = langRef.isObject() ? text(langRef, "key") : resolveTextNode(langRef);
            if (StringUtils.isBlank(langKey))
                continue;

            // Key can be the language code directly or a "/languages/{code}" reference
            String langCode = langKey.contains("/") ? langKey.substring(langKey.lastIndexOf('/') + 1) : langKey;
            return MARC_TO_ISO_639_1.get(langCode.toLowerCase());
        }

        // Fallback: some records expose a top-level "language_code" string field
        String languageCode = text(node, "language_code");
        return (StringUtils.isNotBlank(languageCode)) ? MARC_TO_ISO_639_1.get(languageCode.toLowerCase()) : null;
    }

    /// Extracts the list of author references from a book's JSON node.
    ///
    /// In an Open Library edition document the `authors` array contains either
    /// reference objects (`{"key": "/authors/OL456A"}`) or plain string keys. This
    /// method resolves each entry to a bare OLID and constructs an
    /// `ExternalBook.AuthorRef` with the default [AuthorRole#AUTHOR] role.
    ///
    /// Entries with blank or unresolvable OLIDs are silently skipped.
    ///
    /// @param node the root [JsonNode] of an Open Library edition; may be `null`
    /// @return a list of `ExternalBook.AuthorRef` objects (may be empty,
    ///         never `null`)
    private List<ExternalBook.AuthorRef> extractAuthorRefs(JsonNode node) {
        if (node == null || node.isNull())
            return Collections.emptyList();

        JsonNode authorsNode = node.get("authors");
        if (authorsNode == null || !authorsNode.isArray() || authorsNode.isEmpty())
            return Collections.emptyList();

        List<ExternalBook.AuthorRef> authorRefs = new ArrayList<>();
        for (JsonNode item : authorsNode) {
            if (item == null || item.isNull())
                continue;

            // Author entries can be reference objects {"key": "/authors/OL456A"} or plain
            // strings
            String authorKey = item.isObject() ? text(item, "key") : resolveTextNode(item);
            String authorOlid = extractOlidFromKey(authorKey);

            // Skip entries that cannot be resolved to a valid OLID
            if (StringUtils.isBlank(authorOlid))
                continue;

            authorRefs.add(
                    ExternalBook.AuthorRef.builder()
                            .olid(authorOlid)
                            .role(AuthorRole.AUTHOR)
                            .build());
        }

        return authorRefs;
    }

    // -----------------------------------------------------------------------
    // Generic helpers
    // -----------------------------------------------------------------------

    /// Extracts a 4-digit year from a loosely formatted date string.
    ///
    /// Open Library date fields such as `publish_date`, `birth_date`, and
    /// `death_date` may contain full dates (`"January 1, 2003"`), year-only values
    /// (`"2003"`), approximate dates (`"ca. 1850"`), or arbitrary free text. This
    /// method uses [YEAR_PATTERN] to find the first standalone 4-digit sequence
    /// regardless of surrounding text.
    ///
    /// @param dateStr the raw date string from the API response; may be blank
    /// @return the extracted year as a [Short], or `null` if no 4-digit year could
    ///         be found
    private Short extractYearFromDate(String dateStr) {
        if (StringUtils.isBlank(dateStr))
            return null;

        // Try to extract a 4-digit year from the date string
        Matcher m = YEAR_PATTERN.matcher(dateStr);
        return m.find() ? Short.valueOf(m.group(1)) : null;
    }

    /// Reads a named field from a JSON object node and resolves it to a plain
    /// string.
    ///
    /// Delegates to [resolveTextNode(JsonNode)] to handle both plain strings and
    /// Open Library typed value objects.
    ///
    /// @param node  the parent [JsonNode]; may be `null`
    /// @param field the field name to read
    ///
    /// @return the resolved, stripped string value, or `null` if the field is
    ///         absent, null, blank, or unresolvable
    private String text(JsonNode node, String field) {
        if (node == null || node.isNull())
            return null;

        JsonNode textNode = node.get(field);
        return resolveTextNode(textNode);
    }

    /// Resolves a [JsonNode] to a plain, stripped string.
    ///
    /// Open Library uses two representations for text fields:
    ///
    ///     - **Plain string** – the node itself carries the text value directly.
    ///
    ///     - **Typed value object** – a JSON object of the form `{"type": "/type/text", "value": "actual text"}`;
    ///       the text is nested under the `value` key and extracted via a recursive call.
    ///
    /// Blank strings are normalised to `null` so that callers never receive
    /// whitespace-only values.
    ///
    /// @param textNode the [JsonNode] to resolve; may be `null`
    /// @return the stripped text value, or `null` if the node is null, blank, or
    ///         does not resolve to a non-empty string
    private String resolveTextNode(JsonNode textNode) {
        if (textNode == null || textNode.isNull())
            return null;

        // Plain String
        if (textNode.isString()) {
            String value = textNode.asString().strip();
            return StringUtils.isNotBlank(value) ? value : null;
        }

        // Typed Object: {"type": "...", "value": "..."}
        return resolveTextNode(textNode.get("value"));
    }

    /// Reads a named integer field from a JSON object node.
    ///
    /// @param node  the parent [JsonNode]; may be `null`
    /// @param field the field name to read
    ///
    /// @return the integer value, or `null` if the field is absent, null, or not an
    ///         integer node
    private Integer integer(JsonNode node, String field) {
        if (node == null || node.isNull())
            return null;

        JsonNode valueNode = node.get(field);
        if (valueNode == null || !valueNode.isInt())
            return null;
        return valueNode.asInt();
    }

    /// Returns the first resolvable string value from a JSON array field.
    ///
    /// Iterates over the array elements in order and returns the first non-null
    /// string resolved by [resolveTextNode(JsonNode)]. Useful for fields like
    /// `isbn_10`, `publishers`, or `dewey_decimal_class` where only the primary
    /// (first) value is needed.
    ///
    /// @param node  the parent [JsonNode]; may be `null`
    /// @param field the name of the array field within `node`
    ///
    /// @return the first non-null resolved string in the array, or `null` if the
    ///         field is absent, not an array, empty, or all elements resolve
    ///         to `null`
    private String firstInArray(JsonNode node, String field) {
        if (node == null || node.isNull())
            return null;

        JsonNode arrayNode = node.get(field);
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty())
            return null;

        for (JsonNode itemNode : arrayNode) {
            String value = resolveTextNode(itemNode);
            if (value != null)
                return value;
        }

        return null;
    }

    /// Returns the first non-null value from a variable-length list of candidates.
    ///
    /// Intended as a concise inline fallback chain, e.g.:
    /// ```
    /// firstNonNull(text(node, "personal_name"), text(node, "name"))
    /// ```
    ///
    /// @param <T>    the type of the values
    /// @param values the candidate values to evaluate in order
    ///
    /// @return the first non-null value, or `null` if all values are `null`
    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T v : values)
            if (v != null)
                return v;
        return null;
    }
}
