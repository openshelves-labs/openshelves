package com.openshelves.services.metadata.resolver;

import com.openshelves.model.dto.metadata.ContributorReview;
import com.openshelves.model.dto.metadata.ExternalBook;
import com.openshelves.model.enums.AuthorRole;
import com.openshelves.model.enums.MetadataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/// Resolves per-provider [ExternalBook.AuthorRef] candidates for a single book into a
/// deduplicated list of [ContributorReview]s.
///
/// Matching precedence, most to least authoritative:
///
///   1. OLID equality
///   2. ASIN equality
///   3. Name comparison — direct + token-sorted Jaro-Winkler, with Soundex-based
///      phonetic similarity as a *confirming* signal for moderate-confidence
///      direct matches only (phonetic similarity alone never confirms a match).
@Slf4j
@Component
public class AuthorRoleResolver {

    // -------------------------------------------------------------------------
    // Name-matching thresholds
    // -------------------------------------------------------------------------

    /// Direct/token-sorted Jaro-Winkler score above which two names are confidently the same,
    /// no phonetic backup needed.
    private static final double STRONG_MATCH_THRESHOLD = 0.90;

    /// Direct/token-sorted Jaro-Winkler score above which two names are only accepted as
    /// the same person if phonetic similarity also strongly agrees.
    private static final double MODERATE_MATCH_THRESHOLD = 0.75;

    /// Phonetic Jaro-Winkler score required to confirm a moderate-confidence direct match.
    private static final double PHONETIC_CONFIRM_THRESHOLD = 0.90;

    private static final Set<String> IGNORE_TOKENS = Set.of(
        "mr", "mrs", "ms", "dr", "prof", "jr", "sr", "ii", "iii", "iv"
    );

    private static final Pattern APOSTROPHE = Pattern.compile("['\u2019]");
    // Preserves any-script letters (\p{L}) and hyphens; strips everything else,
    // so compound names like "Smith-Jones" or "Jean-Paul" stay single tokens
    // and non-Latin scripts (CJK, Cyrillic, Devanagari, etc.) are kept intact.
    private static final Pattern NON_ALPHA = Pattern.compile("[^\\p{L}\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final String SOUNDEX_MAPPING = "01230120022455012623010202";

    private static final String OLID_FIELD = "olid";
    private static final String ASIN_FIELD = "asin";

    // =========================================================================
    // Public API
    // =========================================================================

    /// Entry point: takes per-provider candidate lists for a single book and
    /// returns one [ContributorReview] per distinct contributor found.
    public List<ContributorReview> resolve(Map<MetadataProvider, List<ExternalBook.AuthorRef>> candidates) {
        List<ProviderAuthorRef> flattened = validateAndFlatten(candidates);
        List<List<ProviderAuthorRef>> clusters = clusterByIdentity(flattened);

        List<ContributorReview> reviews = new ArrayList<>(clusters.size());
        for (List<ProviderAuthorRef> cluster : clusters) {
            reviews.add(buildContributorReview(cluster));
        }
        return reviews;
    }

    /// Determines whether two [ExternalBook.AuthorRef]s refer to the same real-world contributor.
    /// Role is deliberately ignored — the same person can appear as AUTHOR on one
    /// provider and EDITOR on another; role is reconciled separately per cluster.
    ///
    /// Public: used internally by [#clusterByIdentity], and kept public (rather
    /// than private/package-private) so it stays open for direct unit testing.
    public boolean isSameAuthor(ExternalBook.AuthorRef a, ExternalBook.AuthorRef b) {
        if (a == null || b == null) {
            return false;
        }
        if (a == b) {
            return true;
        }

        Boolean byOlid = compareIfBothPresent(a.getOlid(), b.getOlid());
        if (byOlid != null) {
            return byOlid;
        }

        Boolean byAsin = compareIfBothPresent(a.getAsin(), b.getAsin());
        if (byAsin != null) {
            return byAsin;
        }

        return isSamePersonName(a.getName(), b.getName());
    }

    // =========================================================================
    // Clustering
    // =========================================================================

    /// Flattens per-provider candidates into a single list, dropping any AuthorRef
    /// that violates the name/role contract (as if it were never sent by that
    /// provider at all) so downstream matching, clustering, and resolution can
    /// rely on every AuthorRef having a name and role without re-checking.
    private List<ProviderAuthorRef> validateAndFlatten(Map<MetadataProvider, List<ExternalBook.AuthorRef>> candidatesByProvider) {
        List<ProviderAuthorRef> flattened = new ArrayList<>();
        for (Map.Entry<MetadataProvider, List<ExternalBook.AuthorRef>> entry : candidatesByProvider.entrySet()) {
            List<ExternalBook.AuthorRef> refs = entry.getValue();
            if (refs == null) {
                continue;
            }
            for (ExternalBook.AuthorRef ref : refs) {
                if (ref == null) {
                    continue;
                }
                if (ref.getName() == null || ref.getName().isBlank()) {
                    log.warn("Dropping AuthorRef from {} with no name", entry.getKey());
                    continue;
                }
                if (ref.getRole() == null) {
                    log.warn("Dropping AuthorRef from {} with no role", entry.getKey());
                    continue;
                }
                flattened.add(new ProviderAuthorRef(entry.getKey(), ref));
            }
        }
        return flattened;
    }

    /// Naive O(n^2) clustering: for each ref, join it to the first existing cluster
    /// it matches any member of. Per-book contributor lists are small (single-digit),
    /// so this is fine at this scale; a union-find structure would be the way to go
    /// if this is ever reused for larger (e.g. whole-database) clustering.
    private List<List<ProviderAuthorRef>> clusterByIdentity(List<ProviderAuthorRef> refs) {
        List<List<ProviderAuthorRef>> clusters = new ArrayList<>();

        for (ProviderAuthorRef ref : refs) {
            List<ProviderAuthorRef> matchedCluster = null;
            for (List<ProviderAuthorRef> cluster : clusters) {
                for (ProviderAuthorRef member : cluster) {
                    if (isSameAuthor(ref.ref(), member.ref())) {
                        matchedCluster = cluster;
                        break;
                    }
                }
                if (matchedCluster != null) {
                    break;
                }
            }

            if (matchedCluster != null) {
                matchedCluster.add(ref);
            } else {
                List<ProviderAuthorRef> newCluster = new ArrayList<>();
                newCluster.add(ref);
                clusters.add(newCluster);
            }
        }

        return clusters;
    }

    // =========================================================================
    // Identity comparison (OLID / ASIN)
    // =========================================================================

    /// Returns true/false if both identifiers are present (a real signal either way),
    /// or null if either side is missing the identifier (no signal, fall through).
    private Boolean compareIfBothPresent(String left, String right) {
        if (left == null || right == null) {
            return null;
        }
        return left.strip().equalsIgnoreCase(right.strip());
    }

    // =========================================================================
    // Name comparison: normalize -> direct/sorted Jaro-Winkler -> phonetic backup
    // =========================================================================

    /// Decision logic (agreement-based, not a plain max of scores):
    ///   1. Normalize both names; exact match after normalization -> true.
    ///   2. Compute direct Jaro-Winkler and token-sorted Jaro-Winkler; take the best.
    ///   3. A STRONG match on that score is trusted on its own.
    ///   4. A MODERATE match is only accepted if Soundex-based phonetic similarity
    ///      also strongly agrees (rescues cases like "Catherine"/"Kathryn").
    ///   5. Otherwise -> false. Phonetic similarity alone never confirms a match.
    private boolean isSamePersonName(String rawA, String rawB) {
        if (rawA == null || rawB == null) {
            return false;
        }
        String a = normalizeName(rawA);
        String b = normalizeName(rawB);

        if (a.isEmpty() || b.isEmpty()) {
            // Both names failed to normalize to anything, which happens when a
            // name is entirely single-letter tokens (e.g. "A B") — normalizeName()
            // deliberately drops those as middle initials. That's not the same as
            // blank/garbage input, so fall back to comparing the raw names as-is
            // (trimmed, case-insensitive) rather than declaring a non-match.
            String rawTrimmedA = collapseWhitespace(rawA);
            String rawTrimmedB = collapseWhitespace(rawB);
            if (!rawTrimmedA.isEmpty() && !rawTrimmedB.isEmpty()) {
                return rawTrimmedA.equalsIgnoreCase(rawTrimmedB);
            }
            return false;
        }
        if (a.equals(b)) {
            return true;
        }

        double directScore = jaroWinkler(a, b);
        double sortedScore = jaroWinkler(sortTokens(a), sortTokens(b));
        double bestDirectScore = Math.max(directScore, sortedScore);

        if (bestDirectScore >= STRONG_MATCH_THRESHOLD) {
            return true;
        }

        // One side may be abbreviated ("A Mahanty" vs "Arpan Mahanty") — the
        // dropped-initials normalization above collapses "A Mahanty" down to just
        // "mahanty", losing a whole token's worth of signal, so direct/sorted
        // scoring alone can't recognize this as the same person. Check token
        // alignment (with initials preserved) before falling back to phonetics.
        if (isInitialCompatibleMatch(rawA, rawB)) {
            return true;
        }

        if (bestDirectScore >= MODERATE_MATCH_THRESHOLD) {
            return phoneticSimilarity(a, b) >= PHONETIC_CONFIRM_THRESHOLD;
        }

        return false;
    }

    /// Checks whether two names are the same person modulo one side using an
    /// initial in place of a full given name — e.g. "A Mahanty" vs "Arpan Mahanty",
    /// or "Mahanty A" (reordered) vs "Arpan Mahanty".
    ///
    /// Deliberately conservative: requires equal token counts, and requires at
    /// least (tokenCount - 1) tokens to match *exactly* — only one token is
    /// allowed to be an abbreviation. This lets a single initial stand in for a
    /// full name while still rejecting fully-initialed names like "A B" matching
    /// "Alice Brown" (0 exact-token anchors), which is too weak a signal to trust
    /// as a fallback matcher.
    private boolean isInitialCompatibleMatch(String rawA, String rawB) {
        String[] tokensA = tokenizeKeepingInitials(rawA);
        String[] tokensB = tokenizeKeepingInitials(rawB);

        if (tokensA.length == 0 || tokensA.length != tokensB.length) {
            return false;
        }

        // Sorting aligns both "First Last" vs "Last First" order differences and,
        // conveniently, tends to line up an initial with its corresponding full
        // token since both start with the same letter.
        Arrays.sort(tokensA);
        Arrays.sort(tokensB);

        int exactMatches = 0;
        for (int i = 0; i < tokensA.length; i++) {
            String tokenA = tokensA[i];
            String tokenB = tokensB[i];
            if (tokenA.equals(tokenB)) {
                exactMatches++;
                continue;
            }
            boolean initialCompatible =
                (tokenA.length() == 1 && tokenB.startsWith(tokenA))
                    || (tokenB.length() == 1 && tokenA.startsWith(tokenB));
            if (!initialCompatible) {
                return false;
            }
        }

        // At least one full-token anchor required — protects against names that
        // are entirely initials on one or both sides.
        return exactMatches >= tokensA.length - 1;
    }

    // -------------------------------------------------------------------------
    // Name normalization
    // -------------------------------------------------------------------------

    /// Lowercases, strips diacritics, drops apostrophes (so "O'Neil" ~ "Oneil" ~ "O Neil"
    /// all normalize compatibly), replaces remaining punctuation with spaces, strips
    /// titles/suffixes (Mr, Jr, III, ...), and collapses whitespace.
    ///
    /// When skipInitials is true, single-letter tokens (middle initials) are also
    /// dropped — "John A. Smith" vs "John Smith" would otherwise compare a 3-token
    /// string against a 2-token string and throw off both Jaro-Winkler alignment and
    /// Soundex. When false, initials are kept, since that's exactly what
    /// [#isInitialCompatibleMatch] needs to see.
    private String normalizeName(String name, boolean skipInitials) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", ""); // strip combining diacritical marks
        normalized = normalized.toLowerCase();
        normalized = APOSTROPHE.matcher(normalized).replaceAll("");
        normalized = NON_ALPHA.matcher(normalized).replaceAll(" ");
        normalized = WHITESPACE.matcher(normalized).replaceAll(" ").strip();

        if (normalized.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String token : WHITESPACE.split(normalized)) {
            if (token.isEmpty() || IGNORE_TOKENS.contains(token)) {
                continue; // drop titles/suffixes
            }
            if (skipInitials && token.length() == 1) {
                continue; // drop bare initials
            }
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(token);
        }
        return sb.toString();
    }

    private String normalizeName(String name) {
        return normalizeName(name, true);
    }

    /// Same cleanup as [#normalizeName], but returns individual tokens with
    /// initials kept — used by [#isInitialCompatibleMatch], which needs to see
    /// them rather than the space-joined string.
    private String[] tokenizeKeepingInitials(String name) {
        String normalized = normalizeName(name, false);
        return normalized.isEmpty() ? new String[0] : WHITESPACE.split(normalized);
    }

    /// Trims and collapses internal whitespace only (no case-folding, no
    /// character stripping) — used as a raw fallback comparison when full
    /// normalization empties out an otherwise valid name.
    private String collapseWhitespace(String raw) {
        return WHITESPACE.matcher(raw.strip()).replaceAll(" ");
    }

    /// Splits on whitespace, sorts tokens alphabetically, rejoins — so "First Last"
    /// and "Last First" (or "Last, First") normalize to the same token order.
    private String sortTokens(String normalizedName) {
        String[] tokens = WHITESPACE.split(normalizedName);
        if (tokens.length <= 1) {
            return normalizedName;
        }
        Arrays.sort(tokens);
        return String.join(" ", tokens);
    }

    // =========================================================================
    // Phonetic matching (Soundex, per-token, sorted, joined -> Jaro-Winkler)
    // =========================================================================

    private double phoneticSimilarity(String normalizedA, String normalizedB) {
        String soundexA = tokensToSoundex(normalizedA);
        String soundexB = tokensToSoundex(normalizedB);
        if (soundexA.isEmpty() || soundexB.isEmpty()) {
            return 0.0;
        }
        return jaroWinkler(soundexA, soundexB);
    }

    private String tokensToSoundex(String normalizedName) {
        String[] tokens = WHITESPACE.split(normalizedName);
        String[] codes = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            codes[i] = soundex(tokens[i]);
        }
        Arrays.sort(codes);
        return String.join(" ", codes);
    }

    /// Standard American Soundex, including the H/W adjacency rule: two letters
    /// that map to the same digit but are separated only by H or W are treated
    /// as adjacent (merged), same as if nothing were between them — e.g.
    /// "Ashcraft" and "Ashcroft" both code to A261. Letters separated by a vowel
    /// (or Y) are NOT merged even if they'd otherwise map to the same digit —
    /// vowels reset adjacency normally; only H/W are transparent to it.
    private String soundex(String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        word = word.toUpperCase();
        char[] chars = word.toCharArray();

        StringBuilder code = new StringBuilder();
        code.append(chars[0]);

        char lastDigit = SOUNDEX_MAPPING.charAt(chars[0] - 'A');

        for (int i = 1; i < chars.length && code.length() < 4; i++) {
            char c = chars[i];
            if (c < 'A' || c > 'Z') {
                continue;
            }

            if (c == 'H' || c == 'W') {
                // Transparent to adjacency: don't update lastDigit, so the next
                // real letter is still compared against whatever preceded the H/W.
                continue;
            }

            char digit = SOUNDEX_MAPPING.charAt(c - 'A');
            if (digit != '0' && digit != lastDigit) {
                code.append(digit);
            }
            lastDigit = digit;
        }

        while (code.length() < 4) {
            code.append('0');
        }
        return code.toString();
    }

    // =========================================================================
    // Jaro-Winkler (dependency-free — no new third-party libs per AGENTS.md)
    // =========================================================================

    private double jaroWinkler(String s1, String s2) {
        double jaro = jaro(s1, s2);
        int prefixLength = commonPrefixLength(s1, s2, 4);
        return jaro + (prefixLength * 0.1 * (1 - jaro));
    }

    private double jaro(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }
        int len1 = s1.length();
        int len2 = s2.length();
        if (len1 == 0 || len2 == 0) {
            return 0.0;
        }

        int matchDistance = Math.max(0, Math.max(len1, len2) / 2 - 1);
        boolean[] matched1 = new boolean[len1];
        boolean[] matched2 = new boolean[len2];

        int matches = 0;
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);
            for (int j = start; j < end; j++) {
                if (matched2[j] || s1.charAt(i) != s2.charAt(j)) {
                    continue;
                }
                matched1[i] = true;
                matched2[j] = true;
                matches++;
                break;
            }
        }
        if (matches == 0) {
            return 0.0;
        }

        double transpositions = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!matched1[i]) {
                continue;
            }
            while (!matched2[k]) {
                k++;
            }
            if (s1.charAt(i) != s2.charAt(k)) {
                transpositions++;
            }
            k++;
        }
        transpositions /= 2;

        return ((matches / (double) len1)
            + (matches / (double) len2)
            + ((matches - transpositions) / matches)) / 3.0;
    }

    private int commonPrefixLength(String s1, String s2, int max) {
        int n = Math.min(max, Math.min(s1.length(), s2.length()));
        int i = 0;
        while (i < n && s1.charAt(i) == s2.charAt(i)) {
            i++;
        }
        return i;
    }

    // =========================================================================
    // Cluster -> ContributorReview
    // =========================================================================

    private ContributorReview buildContributorReview(List<ProviderAuthorRef> cluster) {
        List<ContributorReview.Conflict> conflicts = new ArrayList<>();

        String name = resolveName(cluster, conflicts);
        String olid = resolveIdentifier(OLID_FIELD, cluster, ProviderAuthorRef::olid, conflicts);
        String asin = resolveIdentifier(ASIN_FIELD, cluster, ProviderAuthorRef::asin, conflicts);
        List<ContributorReview.RoleAssignment> roles = resolveRoles(cluster);

        return ContributorReview.builder()
            .withName(name)
            .withOlid(olid)
            .withAsin(asin)
            .withRoles(roles)
            .withConflicts(conflicts)
            .build();
    }

    /// Chooses the most suitable name for the cluster: majority vote on normalized
    /// name, tie-broken by the longest (most complete) raw display name within the
    /// winning group. Any name that lost the vote is recorded as a conflict.
    ///
    /// Every AuthorRef reaching this method has a name (see validateAndFlatten()),
    /// so there's always at least one name to choose from. The exception below
    /// guards that invariant rather than falling back to a placeholder name.
    private String resolveName(List<ProviderAuthorRef> cluster, List<ContributorReview.Conflict> conflicts) {
        Map<String, List<ProviderAuthorRef>> byNormalizedName = new LinkedHashMap<>();
        for (ProviderAuthorRef par : cluster) {
            byNormalizedName.computeIfAbsent(normalizeName(par.ref().getName()), _ -> new ArrayList<>()).add(par);
        }

        if (byNormalizedName.isEmpty()) {
            throw new IllegalStateException(
                "Cluster has no AuthorRef with a name; violates the AuthorRef.name contract. Providers: "
                    + cluster.stream().map(ProviderAuthorRef::provider).toList());
        }
        if (byNormalizedName.size() == 1) {
            return byNormalizedName.values().iterator().next().getFirst().ref().getName();
        }

        // Tie-break each group by its own longest raw name (not an arbitrary
        // member), so group selection and final name selection agree.
        List<ProviderAuthorRef> winningGroup = byNormalizedName.values().stream()
            .max(Comparator
                .<List<ProviderAuthorRef>>comparingInt(List::size)
                .thenComparingInt(AuthorRoleResolver::longestNameLength))
            .orElseThrow();

        String resolved = winningGroup.stream()
            .map(par -> par.ref().getName())
            .max(Comparator.comparingInt(String::length))
            .orElseThrow();

        String conflictDescription = describeNameConflict(byNormalizedName, resolved);
        log.debug("Conflicting name values in cluster: {}", conflictDescription);
        conflicts.add(ContributorReview.Conflict.builder().withType(conflictDescription).build());

        return resolved;
    }

    private static int longestNameLength(List<ProviderAuthorRef> group) {
        return group.stream()
            .mapToInt(par -> par.ref().getName().length())
            .max()
            .orElse(0);
    }

    private String describeNameConflict(Map<String, List<ProviderAuthorRef>> byNormalizedName, String resolved) {
        StringBuilder sb = new StringBuilder("NAME_MISMATCH: resolved='").append(resolved).append("'");
        for (List<ProviderAuthorRef> group : byNormalizedName.values()) {
            ProviderAuthorRef sample = group.getFirst();
            if (sample.ref().getName().equals(resolved)) {
                continue;
            }
            sb.append(", ").append(sample.provider()).append("='").append(sample.ref().getName()).append("'");
        }
        return sb.toString();
    }

    /// Resolves a single identifier value for the cluster, recording a conflict if
    /// providers report *different* non-null values. No provider is treated as more
    /// authoritative than another, so byValue is a TreeMap: when values conflict, the
    /// lexicographically-smallest one wins. This is only about picking *something*
    /// deterministically regardless of cluster/map iteration order — it's not a
    /// correctness signal, which is why every conflicting value is still recorded.
    private String resolveIdentifier(
        String fieldName,
        List<ProviderAuthorRef> cluster,
        Function<ProviderAuthorRef, String> extractor,
        List<ContributorReview.Conflict> conflicts) {

        Map<String, List<MetadataProvider>> byValue = new TreeMap<>();
        for (ProviderAuthorRef par : cluster) {
            String value = extractor.apply(par);
            if (value != null && !value.isBlank()) {
                byValue.computeIfAbsent(value, _ -> new ArrayList<>()).add(par.provider());
            }
        }

        if (byValue.isEmpty()) {
            return null;
        }
        if (byValue.size() > 1) {
            StringBuilder sb = new StringBuilder(fieldName.toUpperCase()).append("_MISMATCH: ");
            byValue.forEach((value, providers) -> sb.append(providers).append("='").append(value).append("' "));
            String conflictDescription = sb.toString().strip();
            log.debug("Conflicting {} values in cluster: {}", fieldName, conflictDescription);
            conflicts.add(ContributorReview.Conflict.builder().withType(conflictDescription).build());
        }
        return byValue.keySet().iterator().next();
    }

    /// Groups by role, collecting the providers that reported it. sortOrder follows
    /// first-seen order across the cluster (proxy for billing order until providers
    /// give us an explicit one).
    ///
    /// Every AuthorRef reaching this method has a role (see validateAndFlatten()).
    private List<ContributorReview.RoleAssignment> resolveRoles(List<ProviderAuthorRef> cluster) {
        Map<AuthorRole, List<MetadataProvider>> sourcesByRole = new LinkedHashMap<>();
        for (ProviderAuthorRef par : cluster) {
            sourcesByRole.computeIfAbsent(par.ref().getRole(), _ -> new ArrayList<>()).add(par.provider());
        }

        List<ContributorReview.RoleAssignment> assignments = new ArrayList<>();
        int order = 0;
        for (Map.Entry<AuthorRole, List<MetadataProvider>> entry : sourcesByRole.entrySet()) {
            assignments.add(ContributorReview.RoleAssignment.builder()
                .withRole(entry.getKey())
                .withSortOrder(order++)
                .withSources(entry.getValue())
                .build());
        }
        return assignments;
    }

    /// Pairs an [ExternalBook.AuthorRef] with the provider that supplied it.
    private record ProviderAuthorRef(MetadataProvider provider, ExternalBook.AuthorRef ref) {
        String olid() {
            return ref.getOlid();
        }

        String asin() {
            return ref.getAsin();
        }
    }
}
