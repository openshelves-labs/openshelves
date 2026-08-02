package com.openshelves.services.metadata.resolver.roleassignment;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/// Decides whether two raw author-name strings refer to the same person, and derives the
/// key used to pre-group candidates before matching.
///
/// A name parses into three contiguous regions:
///
/// ```
/// "Sir Arthur C. Doyle Jr"
///     honorific -> sir
///    name parts -> arthur, c., doyle
///        suffix -> jr
/// ```
///
/// Two names match when the honorifics do not conflict, the suffixes do not conflict, and
/// the name parts pair up. Those are the three lines of [#sameName]; everything else in
/// this class implements one of them.
@Component
public class AuthorNameMatcher {

    // ----------------------------------------------------------------
    // Tuning
    // ----------------------------------------------------------------

    /// Jaro-Winkler score at or above which two tokens match with no second signal.
    private static final double STRONG_MATCH_THRESHOLD = 0.90;

    /// Jaro-Winkler score at or above which a match additionally requires phonetic
    /// confirmation. Below this the tokens are treated as different regardless of sound.
    private static final double MODERATE_MATCH_THRESHOLD = 0.75;

    /// Normalized Levenshtein similarity that confirms a moderate match when Double
    /// Metaphone returns no code, i.e. for scripts the encoder cannot read.
    private static final double EDIT_CONFIRM_THRESHOLD = 0.80;

    /// Unpaired `WORD` tokens the longer name may carry and still match — the "added
    /// middle name" allowance, as in "Mary Shelley" vs "Mary Wollstonecraft Shelley".
    ///
    /// This is the most sensitive constant in the class. At 2, "John Smith" matches
    /// "John Smith Xavier Zachary".
    private static final int MAX_UNPAIRED_WORDS = 1;

    /// Word-to-word pairs required before any unpaired `WORD` is tolerated. Without this,
    /// "Smith" would match "John Smith" on a single shared surname.
    private static final int MIN_WORD_PAIRS_FOR_UNPAIRED_WORD = 2;

    /// Winkler prefix bonus: per-character weight, and the prefix length it is capped at.
    private static final double WINKLER_PREFIX_SCALE = 0.1;
    private static final int    WINKLER_PREFIX_LIMIT = 4;

    /// Ranks candidate pairings against each other when several are possible. Only the
    /// ordering matters, not the values: identical text outranks any fuzzy match, and an
    /// initial standing in for a word outranks nothing else.
    private static final double AFFINITY_EXACT   = 2.0;
    private static final double AFFINITY_INITIAL = 0.5;

    // ----------------------------------------------------------------
    // Token vocabulary
    // ----------------------------------------------------------------

    /// Recognized in leading position only, so a surname that reads like a title is left
    /// alone. Compared, not discarded, when both names carry one.
    private static final Set<String> HONORIFICS = Set.of(
        "mr", "mrs", "ms", "miss", "mx", "dr", "prof", "professor",
        "sir", "dame", "lord", "lady", "rev", "reverend", "fr", "capt", "captain"
    );

    /// Recognized in trailing position only. Compared when both names carry one, so that
    /// "John Smith Jr" and "John Smith Sr" resolve to different people rather than to one
    /// merged record.
    ///
    /// Single-letter numerals (I, V, X) are deliberately excluded: nothing distinguishes
    /// them from a trailing initial, and reading the X in "Malcolm X" as a numeral erases
    /// him. The cost is that "John Smith IV" matches "John Smith V".
    private static final Set<String> SUFFIXES = Set.of(
        "jr", "snr", "sr", "phd", "md", "dds", "esq",
        "ii", "iii", "iv", "vi", "vii", "viii", "ix", "xi", "xii"
    );

    /// Nobiliary and toponymic particles — the "van" in van Gogh, the "de" in de Beauvoir.
    /// Providers include or omit these inconsistently, so an unpaired particle never
    /// blocks a match.
    private static final Set<String> PARTICLES = Set.of(
        "van", "von", "de", "del", "della", "di", "da", "du",
        "la", "le", "den", "der", "ten", "ter", "bin", "ibn", "al"
    );

    /// Shared deliberately: `doubleMetaphone` builds its result locally and keeps no state
    /// between calls, so the only mutation is `maxCodeLen`, set once at construction.
    private static final DoubleMetaphone DOUBLE_METAPHONE = createEncoder();

    private static final Pattern APOSTROPHE       = Pattern.compile("['\u2018\u2019]");
    private static final Pattern COMBINING_MARKS  = Pattern.compile("\\p{M}");
    /// Retains letters and digits in any script; everything else, hyphens included,
    /// becomes a separator. Digits are retained so "50 Cent" does not reduce to "cent".
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private static final Pattern WHITESPACE       = Pattern.compile("\\s+");

    private static DoubleMetaphone createEncoder() {
        DoubleMetaphone encoder = new DoubleMetaphone();
        encoder.setMaxCodeLen(6);   // default of 4 blurs longer surnames together
        return encoder;
    }

    // ----------------------------------------------------------------
    // Types
    // ----------------------------------------------------------------

    /// Classification of a single name token. The distinction is load-bearing in two
    /// places: an `INITIAL` may stand in for a `WORD` it prefixes, and an unpaired
    /// `PARTICLE` is forgiven where an unpaired `WORD` may not be.
    private enum Kind { WORD, INITIAL, PARTICLE }

    private record Token(String text, Kind kind) { }

    /// A name split into its three contiguous regions. "Sir Arthur C. Doyle Jr" yields
    /// `honorifics=[sir]`, `parts=[arthur, c, doyle]`, `suffixes=[jr]`.
    private record ParsedName(List<String> honorifics, List<Token> parts, List<String> suffixes) {
        boolean hasNoParts() { return parts.isEmpty(); }
    }

    /// Outcome of pairing two token lists.
    ///
    /// @param pairsFormed   how many pairs were established
    /// @param wordPairs     how many of those joined two full words, rather than an
    ///                      initial standing in for one
    /// @param allExact      whether every pair matched on identical text
    /// @param unpairedWords full words in the longer name left without a partner
    private record Pairing(int pairsFormed, int wordPairs, boolean allExact, int unpairedWords) { }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /// Whether two raw name strings refer to the same person. A null on either side yields
    /// "no match" rather than an exception; providers do send them.
    public boolean sameName(String nameA, String nameB) {
        if (nameA == null || nameB == null) return false;

        ParsedName first = parse(nameA);
        ParsedName second = parse(nameB);
        if (first.hasNoParts() || second.hasNoParts()) return false;

        return markersAgree(first.honorifics(), second.honorifics())
            && markersAgree(first.suffixes(), second.suffixes())
            && partsPair(first.parts(), second.parts());
    }

    /// A key for pre-grouping candidates: lower-cased, diacritics and apostrophes removed,
    /// punctuation and hyphens treated as separators.
    ///
    /// This is a matching key, not a display name — it retains honorifics and suffixes and
    /// discards case and punctuation, so "Sir Arthur Conan Doyle" keys as
    /// `sir arthur conan doyle`. Never surface it to a user.
    ///
    /// **Two names sharing a key are guaranteed to match.** Callers rely on this — grouping
    /// by key and then treating one member as representative of the group is only sound if
    /// the group cannot contain two different people. The key therefore retains everything
    /// that can distinguish one person from another, including honorifics and suffixes:
    /// "John Smith Jr" and "John Smith Sr" must not share a key, or a resolver picking a
    /// representative would merge father and son.
    ///
    /// The guarantee runs one way only. Names that match may still key differently —
    /// "Sir Arthur Conan Doyle" keys apart from "Arthur Conan Doyle" — so grouping by key
    /// under-groups rather than over-groups, and [#sameName] closes the remainder. That is
    /// the safe direction to err in.
    ///
    /// Returns "" for null, mirroring [#sameName]'s tolerance rather than throwing.
    public String groupingKey(String name) {
        if (name == null) return "";
        ParsedName parsed = parse(name);

        StringBuilder key = new StringBuilder();
        for (String honorific : parsed.honorifics()) appendWord(key, honorific);
        for (Token part : parsed.parts())            appendWord(key, part.text());
        for (String suffix : parsed.suffixes())      appendWord(key, suffix);
        return key.toString();
    }

    private static void appendWord(StringBuilder key, String word) {
        if (!key.isEmpty()) key.append(' ');
        key.append(word);
    }

    // ----------------------------------------------------------------
    // Parsing
    // ----------------------------------------------------------------

    private ParsedName parse(String raw) {
        List<String> words = splitToWords(raw);
        if (words.isEmpty()) return new ParsedName(List.of(), List.of(), List.of());

        // Consume honorifics from the front and suffixes from the back. The `> 1` guard
        // prevents consuming the name entirely: a contributor recorded as only "Dr" keeps
        // that as their name rather than parsing to nothing.
        int firstNamePart = 0;
        int afterLastNamePart = words.size();
        while (afterLastNamePart - firstNamePart > 1 && HONORIFICS.contains(words.get(firstNamePart))) {
            firstNamePart++;
        }
        while (afterLastNamePart - firstNamePart > 1 && SUFFIXES.contains(words.get(afterLastNamePart - 1))) {
            afterLastNamePart--;
        }

        List<Token> parts = new ArrayList<>(afterLastNamePart - firstNamePart);
        for (int index = firstNamePart; index < afterLastNamePart; index++) {
            String word = words.get(index);
            parts.add(new Token(word, classify(word)));
        }

        // subList views rather than copies; the three regions are already contiguous.
        return new ParsedName(
            words.subList(0, firstNamePart),
            parts,
            words.subList(afterLastNamePart, words.size()));
    }

    private static List<String> splitToWords(String raw) {
        // NFD splits an accented character into base letter plus combining mark, so
        // removing the marks leaves "Márquez" and "Marquez" identical.
        String text = Normalizer.normalize(raw, Normalizer.Form.NFD);
        text = COMBINING_MARKS.matcher(text).replaceAll("");
        text = text.toLowerCase();
        // Apostrophes are deleted rather than treated as separators: O'Brien is one word.
        text = APOSTROPHE.matcher(text).replaceAll("");
        // Everything else becomes a separator, which is what makes "Smith-Jones" and
        // "Smith Jones" tokenize identically.
        text = NON_ALPHANUMERIC.matcher(text).replaceAll(" ");
        text = WHITESPACE.matcher(text).replaceAll(" ").strip();
        return text.isEmpty() ? List.of() : List.of(WHITESPACE.split(text));
    }

    private static Kind classify(String word) {
        if (PARTICLES.contains(word)) return Kind.PARTICLE;
        boolean singleLetter = word.length() == 1 && Character.isLetter(word.charAt(0));
        return singleLetter ? Kind.INITIAL : Kind.WORD;
    }

    /// Honorifics and suffixes conflict only when both names carry them and they differ.
    /// A marker present on one side alone carries no signal, since providers record them
    /// erratically — hence "Sir Arthur Conan Doyle" matches "Arthur Conan Doyle", while
    /// "Ms Marvel" does not match "Dr Marvel".
    private static boolean markersAgree(List<String> first, List<String> second) {
        return first.isEmpty() || second.isEmpty() || first.equals(second);
    }

    // ----------------------------------------------------------------
    // Name-part matching
    // ----------------------------------------------------------------

    /// Every token of the shorter name must find a partner; leftovers can therefore only
    /// exist in the longer one, and must then be permitted by the unpaired-token policy.
    private boolean partsPair(List<Token> first, List<Token> second) {
        List<Token> shorter = first.size() <= second.size() ? first : second;
        List<Token> longer  = first.size() <= second.size() ? second : first;

        Pairing pairing = pair(shorter, longer);

        return pairing.pairsFormed() == shorter.size()
            && hasIdentityAnchor(pairing)
            && unpairedTokensArePermitted(pairing);
    }

    /// Pairs tokens highest-affinity first: scan every remaining combination, commit the
    /// strongest, repeat. A token with two plausible partners therefore goes to the better
    /// one rather than to whichever appears first in the list.
    ///
    /// Worked example, "Jon Jonson" against "John Johnson", where every token could
    /// plausibly pair with every other and the scores decide it:
    ///
    /// ```
    ///   jonson <-> johnson   0.962   committed first
    ///   jon    <-> john      0.933   committed second
    ///   jon    <-> johnson   0.848   unreachable, johnson already taken
    ///   jonson <-> john      0.800   unreachable, jonson already taken
    /// ```
    ///
    /// Neither positional nor sort-based alignment would do this correctly. Sorting is the
    /// tempting alternative and it fails on "Catherine Hardwick" against "Katherine
    /// Hardwick": catherine sorts before hardwick and katherine after it, so two spellings
    /// of the same name land on opposite sides and are never compared.
    ///
    /// Because the strongest pair is always taken first, and affinity is unchanged by
    /// swapping its arguments, `sameName(x, y)` and `sameName(y, x)` commit identical pairs
    /// in identical order. The result cannot depend on argument order.
    private Pairing pair(List<Token> shorter, List<Token> longer) {
        boolean[] takenFromShorter = new boolean[shorter.size()];
        boolean[] takenFromLonger  = new boolean[longer.size()];

        int pairsFormed = 0;
        int wordPairs = 0;
        boolean allExact = true;

        // At most one pair per token of the shorter list, which bounds the loop.
        for (int remaining = shorter.size(); remaining > 0; remaining--) {
            int bestFromShorter = -1;
            int bestFromLonger = -1;
            double bestAffinity = -1;

            for (int shorterIndex = 0; shorterIndex < shorter.size(); shorterIndex++) {
                if (takenFromShorter[shorterIndex]) continue;
                Token candidate = shorter.get(shorterIndex);

                for (int longerIndex = 0; longerIndex < longer.size(); longerIndex++) {
                    if (takenFromLonger[longerIndex]) continue;
                    Token partner = longer.get(longerIndex);
                    if (!referToSame(candidate, partner)) continue;

                    double score = affinity(candidate, partner);
                    if (score > bestAffinity) {
                        bestAffinity = score;
                        bestFromShorter = shorterIndex;
                        bestFromLonger = longerIndex;
                    }
                }
            }
            if (bestFromShorter < 0) break;   // nothing left that can legally pair

            takenFromShorter[bestFromShorter] = true;
            takenFromLonger[bestFromLonger] = true;
            pairsFormed++;

            Token committedShorter = shorter.get(bestFromShorter);
            Token committedLonger = longer.get(bestFromLonger);
            if (committedShorter.kind() == Kind.WORD && committedLonger.kind() == Kind.WORD) {
                wordPairs++;
            }
            if (!committedShorter.text().equals(committedLonger.text())) {
                allExact = false;
            }
        }

        int unpairedWords = countUnpairedWords(longer, takenFromLonger);
        return new Pairing(pairsFormed, wordPairs, allExact, unpairedWords);
    }

    private static int countUnpairedWords(List<Token> longer, boolean[] taken) {
        int unpaired = 0;
        for (int index = 0; index < longer.size(); index++) {
            if (!taken[index] && longer.get(index).kind() == Kind.WORD) unpaired++;
        }
        return unpaired;
    }

    /// At least one pairing must join two full words. Initials cannot establish identity on
    /// their own: without this rule "A. M." would match "Arpan Mahanty" and equally
    /// "Anil Mehta".
    ///
    /// The exception is a name recorded entirely as initials matching another written the
    /// same way, such as "A. B." and "A B". There the initials are not standing in for
    /// anything — they are the name exactly as the provider holds it.
    private static boolean hasIdentityAnchor(Pairing pairing) {
        return pairing.wordPairs() >= 1 || pairing.allExact();
    }

    /// Unpaired `INITIAL` and `PARTICLE` tokens never block a match: a dropped middle
    /// initial or an omitted "van" says nothing about identity.
    ///
    /// Unpaired `WORD` tokens are the risky case and are rationed. "Mary Shelley" reaching
    /// "Mary Wollstonecraft Shelley" is worth permitting; "John Smith" reaching "John Smith
    /// Xavier Zachary" is not, and one unpaired word is where that line falls.
    private static boolean unpairedTokensArePermitted(Pairing pairing) {
        if (pairing.unpairedWords() == 0) return true;
        if (pairing.unpairedWords() > MAX_UNPAIRED_WORDS) return false;
        return pairing.wordPairs() >= MIN_WORD_PAIRS_FOR_UNPAIRED_WORD;
    }

    // ----------------------------------------------------------------
    // Token-level decision
    // ----------------------------------------------------------------

    /// Whether two tokens refer to the same name-part. An initial matches any token it
    /// prefixes, and that single rule covers every abbreviation case on its own: it is what
    /// makes "A. Mahanty" match "Arpan Mahanty", and equally what lets "J.K. Rowling" reach
    /// "JK Rowling", the initial `j` standing in for the word `jk`.
    private boolean referToSame(Token first, Token second) {
        if (first.kind() == Kind.INITIAL || second.kind() == Kind.INITIAL) {
            Token initial = first.kind() == Kind.INITIAL ? first : second;
            Token word    = first.kind() == Kind.INITIAL ? second : first;
            return word.text().startsWith(initial.text());
        }
        return textsReferToSame(first.text(), second.text());
    }

    /// Exact, or strongly similar, or moderately similar and phonetically confirmed.
    ///
    /// The second signal is deliberately not another spelling measure. Jaro-Winkler and
    /// Levenshtein both quantify character overlap, so confirming one with the other
    /// largely asks the same question twice. Phonetic equivalence is a genuinely different
    /// axis, which is what admits "Geoff"/"Jeff" while rejecting "Arpan"/"Arnab".
    private boolean textsReferToSame(String first, String second) {
        if (first.equals(second)) return true;

        double similarity = jaroWinkler(first, second);
        if (similarity >= STRONG_MATCH_THRESHOLD)   return true;
        if (similarity >= MODERATE_MATCH_THRESHOLD) return phoneticallyConfirms(first, second);
        return false;
    }

    /// Ranks pairings that [#referToSame] has already accepted, purely to decide commit
    /// order. It never determines whether a pairing is permitted.
    private double affinity(Token first, Token second) {
        if (first.text().equals(second.text())) return AFFINITY_EXACT;
        if (first.kind() == Kind.INITIAL || second.kind() == Kind.INITIAL) return AFFINITY_INITIAL;
        return jaroWinkler(first.text(), second.text());
    }

    /// Double Metaphone reads Latin script only and returns an empty code otherwise, so
    /// Cyrillic and CJK fall back to edit distance. That fallback exists because those
    /// scripts cannot be encoded at all — it is not a second opinion on Latin text.
    private boolean phoneticallyConfirms(String first, String second) {
        String firstPrimary = DOUBLE_METAPHONE.doubleMetaphone(first);
        String secondPrimary = DOUBLE_METAPHONE.doubleMetaphone(second);

        if (isBlank(firstPrimary) || isBlank(secondPrimary)) {
            return normalizedLevenshtein(first, second) >= EDIT_CONFIRM_THRESHOLD;
        }

        // Each word carries a primary and an alternate pronunciation, for names that cross
        // languages. A match on any combination counts.
        String firstAlternate = DOUBLE_METAPHONE.doubleMetaphone(first, true);
        String secondAlternate = DOUBLE_METAPHONE.doubleMetaphone(second, true);
        return firstPrimary.equals(secondPrimary)
            || firstPrimary.equals(secondAlternate)
            || firstAlternate.equals(secondPrimary)
            || firstAlternate.equals(secondAlternate);
    }

    private static boolean isBlank(String code) {
        return code == null || code.isEmpty();
    }

    // ----------------------------------------------------------------
    // Spelling similarity
    //
    // Two standard published measures. Internal variable names follow the source papers
    // rather than house style, so the implementations can be read against the reference;
    // both are pinned by tests against published values (Martha/Marhta, Dwayne/Duane,
    // Dixon/Dicksonx).
    // ----------------------------------------------------------------

    /// Edit distance rescaled to 1.0 for identical and 0.0 for entirely dissimilar.
    private static double normalizedLevenshtein(String first, String second) {
        int maxLength = Math.max(first.length(), second.length());
        if (maxLength == 0) return 1.0;
        return 1.0 - (levenshtein(first, second) / (double) maxLength);
    }

    /// Single-character edits needed to turn one string into the other. Two rolling rows
    /// rather than a full matrix, since only the previous row is ever read.
    private static int levenshtein(String first, String second) {
        int[] previousRow = new int[second.length() + 1];
        int[] currentRow = new int[second.length() + 1];
        for (int column = 0; column <= second.length(); column++) previousRow[column] = column;

        for (int row = 1; row <= first.length(); row++) {
            currentRow[0] = row;
            for (int column = 1; column <= second.length(); column++) {
                int substitutionCost = first.charAt(row - 1) == second.charAt(column - 1) ? 0 : 1;
                currentRow[column] = Math.min(
                    Math.min(currentRow[column - 1] + 1,        // insertion
                        previousRow[column] + 1),          // deletion
                    previousRow[column - 1] + substitutionCost);
            }
            int[] swap = previousRow;
            previousRow = currentRow;
            currentRow = swap;
        }
        return previousRow[second.length()];
    }

    /// Jaro, plus a bonus for a shared prefix. Names that begin alike usually are alike,
    /// which is the bias worth having when comparing them.
    private static double jaroWinkler(String first, String second) {
        double jaro = jaro(first, second);
        int sharedPrefix = commonPrefixLength(first, second, WINKLER_PREFIX_LIMIT);
        return jaro + (sharedPrefix * WINKLER_PREFIX_SCALE * (1 - jaro));
    }

    /// Counts characters the two strings share within a sliding window, then penalises
    /// shared characters that appear in swapped order.
    private static double jaro(String first, String second) {
        if (first.equals(second)) return 1.0;
        int firstLength = first.length();
        int secondLength = second.length();
        if (firstLength == 0 || secondLength == 0) return 0.0;

        // How far apart two characters may sit and still count as the same one.
        int window = Math.max(0, Math.max(firstLength, secondLength) / 2 - 1);
        boolean[] matchedInFirst = new boolean[firstLength];
        boolean[] matchedInSecond = new boolean[secondLength];
        int matches = 0;

        for (int index = 0; index < firstLength; index++) {
            int windowStart = Math.max(0, index - window);
            int windowEnd = Math.min(index + window + 1, secondLength);
            for (int candidate = windowStart; candidate < windowEnd; candidate++) {
                if (matchedInSecond[candidate] || first.charAt(index) != second.charAt(candidate)) continue;
                matchedInFirst[index] = matchedInSecond[candidate] = true;
                matches++;
                break;
            }
        }
        if (matches == 0) return 0.0;

        // Walk both sets of matches in order; wherever they disagree, that pair was swapped.
        double transpositions = 0;
        int secondCursor = 0;
        for (int index = 0; index < firstLength; index++) {
            if (!matchedInFirst[index]) continue;
            while (!matchedInSecond[secondCursor]) secondCursor++;
            if (first.charAt(index) != second.charAt(secondCursor)) transpositions++;
            secondCursor++;
        }
        transpositions /= 2;   // each swap was counted from both ends

        return (matches / (double) firstLength
            + matches / (double) secondLength
            + (matches - transpositions) / matches) / 3.0;
    }

    private static int commonPrefixLength(String first, String second, int maxLength) {
        int limit = Math.min(maxLength, Math.min(first.length(), second.length()));
        int length = 0;
        while (length < limit && first.charAt(length) == second.charAt(length)) length++;
        return length;
    }
}
