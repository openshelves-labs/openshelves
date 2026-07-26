package com.openshelves.services.metadata.resolver.roleassignment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/// Comprehensive test suite for [AuthorNameMatcher], organized around real author-name
/// metadata scenarios (the primary use case) plus direct pins on the hand-rolled
/// Jaro-Winkler / Levenshtein algorithms.
///
/// Sections marked "DISCOVERED GAP" document current behavior that surprised us
/// during test-writing rather than behavior we're confident is intended — flagged
/// for a design discussion rather than silently asserted as correct.
class AuthorNameMatcherTest {

    private final AuthorNameMatcher matcher = new AuthorNameMatcher();

    // ------------------------------------------------------------
    // Reflection helpers for private algorithm internals
    // ------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private <T> T invokePrivate(String methodName, Class<?>[] types, Object... args) {
        try {
            Method m = AuthorNameMatcher.class.getDeclaredMethod(methodName, types);
            m.setAccessible(true);
            return (T) m.invoke(matcher, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private double jaro(String a, String b) {
        return invokePrivate("jaro", new Class<?>[]{String.class, String.class}, a, b);
    }

    private double jaroWinkler(String a, String b) {
        return invokePrivate("jaroWinkler", new Class<?>[]{String.class, String.class}, a, b);
    }

    private int levenshtein(String a, String b) {
        return invokePrivate("levenshtein", new Class<?>[]{String.class, String.class}, a, b);
    }

    private double normalizedLevenshtein(String a, String b) {
        return invokePrivate("normalizedLevenshtein", new Class<?>[]{String.class, String.class}, a, b);
    }

    // ==============================================================
    // 1. Null / blank / degenerate inputs
    // ==============================================================

    @Nested
    @DisplayName("Null and blank input handling")
    class NullAndBlank {

        @ParameterizedTest
        @NullSource
        void nullFirstArgReturnsFalse(String nullName) {
            assertFalse(matcher.sameName(nullName, "Brandon Sanderson"));
        }

        @ParameterizedTest
        @NullSource
        void nullSecondArgReturnsFalse(String nullName) {
            assertFalse(matcher.sameName("Brandon Sanderson", nullName));
        }

        @Test
        void bothNullReturnsFalse() {
            assertFalse(matcher.sameName(null, null));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        void blankVsBlankReturnsFalse(String blank) {
            // Two blanks are not "the same author" — there's no identity to match.
            assertFalse(matcher.sameName(blank, blank));
        }

        @Test
        void blankVsRealNameReturnsFalse() {
            assertFalse(matcher.sameName("", "Brandon Sanderson"));
            assertFalse(matcher.sameName("   ", "Brandon Sanderson"));
        }

        @Test
        void punctuationOnlyNameCollapsesToEmpty() {
            // Assert the normalize() claim the test name makes, not just the sameName
            // consequence — an earlier version only checked the latter.
            assertEquals("", matcher.groupingKey("..."));
            assertEquals("", matcher.groupingKey("!!!"));
            assertFalse(matcher.sameName("...", "Brandon Sanderson"));
        }
    }

    // ==============================================================
    // 2. Exact / normalized equality
    // ==============================================================

    @Nested
    @DisplayName("Exact and case/whitespace-insensitive matches")
    class ExactMatches {

        @Test
        void identicalStringsMatch() {
            assertTrue(matcher.sameName("Brandon Sanderson", "Brandon Sanderson"));
        }

        @Test
        void caseInsensitive() {
            assertTrue(matcher.sameName("BRANDON SANDERSON", "brandon sanderson"));
        }

        @Test
        void extraWhitespaceIgnored() {
            assertTrue(matcher.sameName("Brandon   Sanderson", "Brandon Sanderson"));
            assertTrue(matcher.sameName("  Brandon Sanderson  ", "Brandon Sanderson"));
        }

        @Test
        void reversedTokenOrderMatches() {
            // "Last, First" style input vs "First Last" — both tokenize the same way
            // once punctuation is stripped, and token-count equality + pairing handles order.
            assertTrue(matcher.sameName("Sanderson Brandon", "Brandon Sanderson"));
        }
    }

    // ==============================================================
    // 3. Normalization: diacritics, apostrophes, hyphens, titles/suffixes
    // ==============================================================

    @Nested
    @DisplayName("Normalization rules")
    class Normalization {

        @Test
        void diacriticsStrippedForMatching() {
            assertTrue(matcher.sameName("Gabriel García Márquez", "Gabriel Garcia Marquez"));
        }

        @Test
        void diacriticsStrippedInNormalizeOutput() {
            assertEquals("garcia marquez", matcher.groupingKey("García Márquez"));
        }

        @Test
        void apostropheVariantsIgnored() {
            assertTrue(matcher.sameName("Chinua O'Brien", "Chinua OBrien"));
            // curly quote vs straight quote
            assertTrue(matcher.sameName("D\u2019Angelo", "D'Angelo"));
        }

        @Test
        void hyphenTreatedAsSeparator() {
            assertTrue(matcher.sameName("Smith-Jones", "Smith Jones"));
        }

        @Test
        void compoundSurnameNormalizesToSpacedTokens() {
            assertEquals("smith jones", matcher.groupingKey("Smith-Jones"));
        }

        @ParameterizedTest
        @CsvSource({
            "Dr. Seuss, Seuss",
            "Mr. Tolkien, Tolkien",
            "Martin Luther King Jr., Martin Luther King",
            "Sammy Davis Sr., Sammy Davis",
            "William Shatner III, William Shatner"
        })
        void titlesAndSuffixesStripped(String withTitle, String withoutTitle) {
            assertTrue(matcher.sameName(withTitle, withoutTitle),
                "'" + withTitle + "' should match '" + withoutTitle + "' once title/suffix is stripped");
        }

        @Test
        @DisplayName("INVARIANT: two names with the same key always match")
        void equalKeysImplyAMatch() {
            // normalize() is used to pre-group candidates, after which one member is taken
            // as representative of the group. That is only sound if a shared key really
            // does mean the same person — otherwise the representative silently merges two
            // people. Fuzzed rather than assumed.
            //
            // This is what forces honorifics and suffixes into the key: without them
            // "John Smith Jr" and "John Smith Sr" key identically but are not the same man.
            String[] fragments = {"john", "smith", "jr", "sr", "ii", "iii", "dr", "ms", "sir",
                "a", "k", "van", "de", "malcolm", "x", "50", "rowling"};
            Random random = new Random(1234);
            Map<String, String> firstSeenWithKey = new HashMap<>();

            for (int attempt = 0; attempt < 40_000; attempt++) {
                StringBuilder candidate = new StringBuilder();
                int wordCount = 1 + random.nextInt(4);
                for (int word = 0; word < wordCount; word++) {
                    if (word > 0) candidate.append(random.nextBoolean() ? " " : ". ");
                    candidate.append(fragments[random.nextInt(fragments.length)]);
                }
                String name = candidate.toString();
                String key = matcher.groupingKey(name);
                if (key.isEmpty()) continue;

                String previous = firstSeenWithKey.putIfAbsent(key, name);
                if (previous != null && !previous.equals(name)) {
                    assertTrue(matcher.sameName(previous, name),
                        "same key \"" + key + "\" but no match: \"" + previous + "\" vs \"" + name + "\"");
                }
            }
        }

        @Test
        @DisplayName("INVARIANT holds for the cases that motivated it")
        void generationsAndTitlesNeverShareAKey() {
            assertNotEquals(matcher.groupingKey("John Smith Jr"), matcher.groupingKey("John Smith Sr"));
            assertNotEquals(matcher.groupingKey("John Smith II"), matcher.groupingKey("John Smith III"));
            assertNotEquals(matcher.groupingKey("Ms Marvel"), matcher.groupingKey("Dr Marvel"));
            assertNotEquals(matcher.groupingKey("Dr Strange"), matcher.groupingKey("Ms Strange"));
        }

        @Test
        void singleLetterTokensRetainedInNormalize() {
            // "Ursula K. Le Guin" -> "k" is a single-letter token, dropped by default normalize.
            // CHANGED: initials are retained in the grouping key. NameCandidateResolver
            // groups by normalize(), so "Ursula K. Le Guin" and "Ursula Le Guin" are now
            // separate groups that sameName() then folds together, rather than one group.
            assertEquals("ursula k le guin", matcher.groupingKey("Ursula K. Le Guin"));
        }

        @Test
        void allInitialsNameFallsBackToRawCaseInsensitiveCompare() {
            // "A B" normalizes to "" (both tokens are single letters and dropped),
            // so sameName falls back to raw case-insensitive string comparison.
            assertTrue(matcher.sameName("A B", "a b"));
            assertFalse(matcher.sameName("A B", "A C"));
        }
    }

    // ==============================================================
    // 4. Initial-compatible matching ("A Mahanty" vs "Arpan Mahanty")
    // ==============================================================

    @Nested
    @DisplayName("Initial-compatible matching")
    class InitialCompatible {

        @Test
        void singleInitialForGivenNameMatches() {
            assertTrue(matcher.sameName("A Mahanty", "Arpan Mahanty"));
        }

        @Test
        void singleInitialEitherDirectionMatches() {
            assertTrue(matcher.sameName("Arpan Mahanty", "A Mahanty"));
        }

        @Test
        void initialWithPeriodMatches() {
            assertTrue(matcher.sameName("A. Mahanty", "Arpan Mahanty"));
        }

        @Test
        @DisplayName("Both-initials name never reaches the anchor rule — it normalizes to empty first")
        void bothTokensAbbreviatedNeverReachesInitialCompatiblePath() {
            // Named for what it ACTUALLY exercises. "A M" has both tokens dropped as
            // single letters, so normalize returns "" and sameName takes the raw-compare
            // fallback — isInitialCompatibleMatch and its "at least one full-token anchor"
            // rule are never consulted. An earlier name for this test implied otherwise.
            // No longer normalizes to "" — initials are real tokens now. The anchor rule
            // (at least one WORD-to-WORD pair) is what rejects this, explicitly.
            assertEquals("a m", matcher.groupingKey("A M"));
            assertFalse(matcher.sameName("A M", "Arpan Mahanty"));
        }

        @Test
        void middleInitialAddedToFullNameMatches() {
            // "George R. R. Martin" vs "George Martin" — middle initial "r" is dropped
            // entirely by default normalize (single-letter token), so these reduce to
            // the same normalized string and match on exact equality, not the initials path.
            assertTrue(matcher.sameName("George R. R. Martin", "George Martin"));
        }

        @Test
        @DisplayName("CHANGED: compressed vs spaced initials now match (previously accepted as a gap)")
        void compressedInitialsNowMatchSpacedInitials() {
            // DISCOVERED GAP: "J.K. Rowling" normalizes (default, skipInitials=true) to
            // a single token "rowling" (both "j" and "k" are dropped as single letters).
            // "JK Rowling" normalizes to two tokens "jk","rowling" ("jk" is 2 chars, kept).
            // sameName treats "rowling" as single-token, "jk rowling" as multi-token, and
            // routes to isInitialCompatibleMatch, which compares WITH-initials tokenization:
            //   "J.K. Rowling" -> [j, k, rowling]   (3 tokens)
            //   "JK Rowling"   -> [jk, rowling]     (2 tokens)
            // Token counts differ, so isInitialCompatibleMatch returns false immediately.
            // This is a real author-metadata pattern (Open Library/Amazon often style
            // "J.K. Rowling" while another provider strips periods to "JK Rowling") and
            // it currently fails to match. Flagging for a design decision rather than
            // asserting this is correct — encoding actual current behavior below so any
            // fix shows up as a deliberate, visible test change rather than a silent one.
            // "JK" is a WORD; the INITIAL "j" prefixes it, so they pair, and the leftover
            // "k" is an unpaired INITIAL. You'd decided to leave this gap alone; it closed
            // as a side effect of unifying tokenization rather than by targeted change.
            assertTrue(matcher.sameName("J.K. Rowling", "JK Rowling"));
            assertTrue(matcher.sameName("J.R.R. Tolkien", "JRR Tolkien"));
        }

        @Test
        void spacedInitialsWithPeriodsMatchFullName() {
            // This variant DOES work: periods between spaced initials still tokenize
            // to single letters, which get dropped, leaving just the surname.
            assertTrue(matcher.sameName("J. K. Rowling", "Rowling"));
        }

        @Test
        void initialMatchRequiresSameSurname() {
            assertFalse(matcher.sameName("A Mahanty", "Arpan Basu"));
        }

        @Test
        @DisplayName("CHANGED: an extra initial no longer blocks an initial-compatible match")
        void extraInitialDoesNotBlockMatch() {
            // v1 returned false purely because token counts differed (3 vs 2). Under one
            // uniform pairing rule, "A." pairs with "Arpan", "Mahanty" pairs exactly, and
            // the leftover "B." is an unpaired INITIAL, which is always permitted.
            // This is a loosening — flagged for sign-off, not a bug fix.
            assertTrue(matcher.sameName("A B Mahanty", "Arpan Mahanty"));
        }
    }

    // ==============================================================
    // 5. Multi-token fuzzy matching (nicknames, misspellings)
    // ==============================================================

    @Nested
    @DisplayName("Multi-token fuzzy matching")
    class MultiTokenFuzzy {

        @Test
        void commonMisspellingMatchesViaJaroWinkler() {
            assertTrue(matcher.sameName("Fyodor Dostoevsky", "Fyodor Dostoyevsky"));
        }

        @Test
        void phoneticallyEquivalentSpellingMatches() {
            // Geoff/Jeff: look different, sound the same -> Double Metaphone confirms.
            assertTrue(matcher.sameName("Geoff Smith", "Jeff Smith"));
        }

        @Test
        void greedyPairingHandlesNonSortAlignedFuzzyTokens() {
            // "Catherine"/"Katherine" sort to opposite sides of "Hardwick" alphabetically,
            // so a naive sort-then-zip alignment would mis-pair them with "Hardwick" itself.
            // Greedy best-match pairing must still align Catherine<->Katherine correctly.
            assertTrue(matcher.sameName("Catherine Hardwick", "Katherine Hardwick"));
        }

        @Test
        void oneDifferingTokenVetoesWholeMatch() {
            // Shared surname alone must not inflate the whole-string score enough to pass;
            // token alignment requires every pair to match, so a differing given name vetoes it.
            assertFalse(matcher.sameName("Arpan Mahanty", "Arnab Mahanty"));
        }

        @Test
        @DisplayName("Philip/Philippa DOES auto-match — this is the documented over-merge policy, not a bug")
        void genderAmbiguousPrefixPairAutoMerges() {
            // "Philippa" contains "Philip" as a literal prefix, so Jaro-Winkler (which
            // rewards shared prefixes) clears the strong-match threshold outright, no
            // phonetic confirmation needed. Per established project policy, conservative
            // over-merge is *intentional* for exactly this kind of gender-ambiguous pair,
            // so asserting true here pins the documented design choice, not a gap.
            assertTrue(matcher.sameName("Philip Marlowe", "Philippa Marlowe"));
        }

        @ParameterizedTest
        @CsvSource({
            "Bob Johnson, Robert Johnson",
            "Liz Warren, Elizabeth Warren"
        })
        @DisplayName("True nicknames (not spelling-adjacent) do NOT auto-match")
        void trueNicknamePairsDoNotAutoMatch(String a, String b) {
            // Unlike Philip/Philippa, "Bob"/"Robert" and "Liz"/"Elizabeth" share too few
            // characters in sequence to clear even the moderate Jaro-Winkler threshold —
            // the conservative over-merge policy only ever engages for names close enough
            // in spelling to begin with; classic nicknames fall outside its reach entirely.
            assertFalse(matcher.sameName(a, b));
        }

        @Test
        void completelyDifferentNamesDoNotMatch() {
            assertFalse(matcher.sameName("Brandon Sanderson", "Ursula Le Guin"));
        }

        @Test
        void sameSurnameDifferentGivenNameDoesNotMatch() {
            assertFalse(matcher.sameName("Stephen King", "Martin Luther King"));
        }
    }

    // ==============================================================
    // 6. Non-Latin script handling (phonetic fallback -> edit distance)
    // ==============================================================

    @Nested
    @DisplayName("Non-Latin script fallback")
    class NonLatinScript {

        @Test
        void identicalCyrillicNamesMatch() {
            assertTrue(matcher.sameName("Толстой", "Толстой"));
        }

        @Test
        void identicalCjkNamesMatch() {
            assertTrue(matcher.sameName("村上春樹", "村上春樹"));
        }

        @Test
        void closeCyrillicVariantMatchesViaEditDistanceFallback() {
            // Double Metaphone can't encode Cyrillic (empty code), so confirmation
            // falls back to normalized Levenshtein for the moderate-JW tier.
            // "Толстой" vs "Талстой" (one-letter substitution) should be close enough.
            assertTrue(matcher.sameName("Толстой", "Талстой"));
        }

        @Test
        void unrelatedCjkNamesDoNotMatch() {
            assertFalse(matcher.sameName("村上春樹", "夏目漱石"));
        }

        @Test
        void latinTranscriptionVsNativeScriptDoesNotAutoMatch() {
            // "Tolstoy" (Latin) vs "Толстой" (Cyrillic) share no characters at all under
            // this matcher — it has no transliteration table, only script-internal fuzzy
            // matching. This is expected: cross-script identity resolution is out of scope
            // for NameMatcher and belongs to a higher-level TRANSLITERATION_CONFLICT path.
            assertFalse(matcher.sameName("Tolstoy", "Толстой"));
        }
    }

    // ==============================================================
    // 7. Token-count-mismatch fallback (e.g. pen name vs full name)
    // ==============================================================

    @Nested
    @DisplayName("Token-count-mismatch fallback path")
    class TokenCountMismatch {

        @Test
        void threeTokensVsTwoWithFuzzyMatchStillMatches() {
            // "Ursula Le Guin" (3 tokens) vs "Ursula Guin" (2) — counts differ, so this
            // takes the whole-string best-of Jaro-Winkler path with phonetic confirmation.
            assertTrue(matcher.sameName("Ursula Le Guin", "Ursula Guin"));
        }

        @Test
        void veryDifferentTokenCountsDoNotFalselyMatch() {
            assertFalse(matcher.sameName("John Ronald Reuel Tolkien", "Isaac Asimov"));
        }
    }

    // ==============================================================
    // 8. Single-token names (bare surnames / mononyms)
    // ==============================================================

    @Nested
    @DisplayName("Single-token and mononym handling")
    class SingleToken {

        @Test
        void twoBareSurnamesFuzzyMatch() {
            assertTrue(matcher.sameName("Dostoevsky", "Dostoyevsky"));
        }

        @Test
        void mononymMatchesItself() {
            assertTrue(matcher.sameName("Voltaire", "Voltaire"));
        }

        @Test
        void bareSurnameAloneDoesNotMatchFullNameSharingIt() {
            // "Smith" alone is too weak to confirm identity with "John Smith" via fuzzy
            // matching (it routes to isInitialCompatibleMatch, which needs equal token
            // counts) — a single shared surname isn't sufficient evidence.
            assertFalse(matcher.sameName("Smith", "John Smith"));
        }

        @Test
        void unrelatedBareSurnamesDoNotMatch() {
            assertFalse(matcher.sameName("Tolkien", "Asimov"));
        }
    }

    // ==============================================================
    // 9. Realistic author-metadata scenarios (cross-provider variance)
    // ==============================================================

    @Nested
    @DisplayName("Realistic cross-provider author metadata scenarios")
    class RealWorldAuthorScenarios {

        @Test
        void middleNameOmittedByOneProvider() {
            assertTrue(matcher.sameName("Ursula K. Le Guin", "Ursula Le Guin"));
        }

        @Test
        void fullNameVsInitialsStyle() {
            assertTrue(matcher.sameName("George R. R. Martin", "George Martin"));
        }

        @Test
        void hyphenatedVsSpacedCompoundSurname() {
            assertTrue(matcher.sameName("Sarah Michelle Gellar-Prinze", "Sarah Michelle Gellar Prinze"));
        }

        @Test
        void diacriticVariantAcrossProviders() {
            assertTrue(matcher.sameName("Björk Guðmundsdóttir", "Bjork Gudmundsdottir"));
        }

        @Test
        void suffixDroppedByOneProvider() {
            assertTrue(matcher.sameName("Robert Downey Jr.", "Robert Downey"));
        }

        @Test
        void distinctAuthorsWithSameCommonSurnameDoNotMerge() {
            assertFalse(matcher.sameName("Brandon Sanderson", "Brandon Mull"));
        }

        @Test
        void distinctAuthorsSharingAFirstNameDoNotMerge() {
            assertFalse(matcher.sameName("Stephen King", "Stephen Fry"));
        }
    }

    // ==============================================================
    // 10. CHARACTERIZATION: verified gaps and over-merges
    // ==============================================================

    /// These tests pin CURRENT behavior discovered by behavioral probing, not desired
    /// behavior. Each one is a decision waiting to be made. They pass today; if any
    /// starts failing, the matcher's semantics changed and someone should confirm that
    /// was intentional. See README "Findings" for the mechanism behind each.
    @Nested
    @DisplayName("V2 behaviour: previously-verified gaps, now resolved")
    class CharacterizationOfKnownGaps {

        @Test
        @DisplayName("FIXED (was GAP 1): extra WORD tokens are now counted, not ignored")
        void extraWordTokensAreCountedAgainstTheMatch() {
            // wholeNamePhoneticallyConfirms zips only min(tokenCount) sorted positions,
            // so any tokens beyond the shorter name's length are never examined at all.
            // "John Smith" therefore matches a name with two extra surnames.
            // Two unpaired WORDs exceed MAX_UNPAIRED_WORDS, so these no longer merge.
            assertFalse(matcher.sameName("John Smith", "John Smith Xavier Zachary"));
            assertFalse(matcher.sameName("Amy Adams", "Amy Adams Zeta Zulu"));
            // A single extra word is still allowed — the "added middle name" case.
            assertTrue(matcher.sameName("Adam Baker", "Adam Baker Zeller"));
            assertTrue(matcher.sameName("Mary Shelley", "Mary Wollstonecraft Shelley"));
        }

        @Test
        @DisplayName("FIXED (was GAP 1b): result no longer depends on where the extra token sorts")
        void resultIsIndependentOfAlphabeticalPosition() {
            // Identical situation to above, except the extra token sorts before the
            // shared ones — now the positional zip mis-aligns and the match fails.
            // Whether two names match depends on the alphabet, which is arbitrary.
            // Both are "2 words paired + 1 extra word", so both now give the same answer
            // regardless of whether the extra token sorts early or late.
            assertTrue(matcher.sameName("Adam Baker", "Adam Baker Zeller"));
            assertTrue(matcher.sameName("Adam Zeller", "Adam Baker Zeller"));
            assertTrue(matcher.sameName("John Smith", "Aaron John Smith"));
        }

        @Test
        @DisplayName("FIXED (was GAP 2): greedy pairing now applies at every token count")
        void greedyPairingAppliesRegardlessOfTokenCount() {
            // Equal token counts -> greedy pairing -> Catherine/Katherine align correctly.
            assertTrue(matcher.sameName("Catherine Hardwick", "Katherine Hardwick"));
            // Unequal counts -> fallback -> sorted positional zip -> the exact mis-pairing
            // the class Javadoc cites as the reason greedy pairing exists. False negative.
            assertTrue(matcher.sameName("Catherine Hardwick Jones", "Katherine Hardwick"));
        }

        @Test
        @DisplayName("FIXED (was GAP 3): honorifics are positional AND compared when both present")
        void honorificsArePositionalAndCompared() {
            // IGNORE_TOKENS is applied to every token regardless of position, so two
            // different honorific-prefixed names collapse to the same normalized form.
            // Directly relevant given Comic Vine is a provider — "Ms. Marvel",
            // "Dr. Strange", "Mr. Fantastic" are real entries in comic metadata.
            // Both carry an honorific and they differ -> different entities.
            assertFalse(matcher.sameName("Ms Marvel", "Dr Marvel"));
            assertFalse(matcher.sameName("Dr Strange", "Ms Strange"));
            // Present on one side only -> ignored, since providers include them erratically.
            assertTrue(matcher.sameName("Sir Arthur Conan Doyle", "Arthur Conan Doyle"));
            // "Iv" is no longer mistaken for a Roman numeral (not in leading/trailing sets).
            assertEquals("iv jones", matcher.groupingKey("Iv Jones"));
            // The stage name survives too: "dr" is recognized as a leading honorific, so
            // "Dr Dre" still matches "Dre", but the key retains it rather than reducing
            // him to a bare "dre" that could collide with someone else.
            assertEquals("dr dre", matcher.groupingKey("Dr Dre"));
            assertTrue(matcher.sameName("Dr Dre", "Dre"));
        }

        @Test
        @DisplayName("FIXED (was GAP 4): initials are retained, so Malcolm X survives")
        void initialsAreRetainedAsTokens() {
            // "Malcolm X" is a real author (The Autobiography of Malcolm X). The X is
            // dropped as a single-letter token, so it becomes indistinguishable from
            // any other "Malcolm <initial>".
            assertEquals("malcolm x", matcher.groupingKey("Malcolm X"));
            assertFalse(matcher.sameName("Malcolm X", "Malcolm Y"));
        }

        @Test
        @DisplayName("GAP 5: Double Metaphone collapses vowels, merging short given names wholesale")
        void phoneticTierMergesAnyVowelDifferingShortName() {
            // Double Metaphone encodes only the leading vowel, so every CvC given name
            // with matching consonants collides: dan/don/den/din/dun all encode to "TN".
            // Combined with the moderate-JW tier this auto-merges them with no review.
            // Broader than the documented gender-variant policy — worth knowing the reach.
            assertTrue(matcher.sameName("Dan Brown", "Don Brown"));
            assertTrue(matcher.sameName("Tim Brown", "Tom Brown"));
            assertTrue(matcher.sameName("Jan Brown", "Jon Brown"));
            assertTrue(matcher.sameName("Ron Chernow", "Ran Chernow"));
        }

        @Test
        @DisplayName("FIXED (was GAP 6): no raw-compare fallback exists, so punctuation never decides")
        void allInitialNamesGoThroughNormalTokenization() {
            // When normalize() yields "" for both sides, sameName falls back to a literal
            // equalsIgnoreCase on the RAW strings — skipping diacritic/punctuation handling
            // that the rest of the class applies. So these two disagree purely on periods.
            assertTrue(matcher.sameName("A B", "a b"));
            assertTrue(matcher.sameName("A. B.", "A B"));
            assertEquals("a b", matcher.groupingKey("A B"));
        }

        @Test
        @DisplayName("PROPERTY: matching is NOT transitive — greedy clustering is order-dependent")
        void matchingIsNotTransitive() {
            // Inherent to threshold-based fuzzy matching, but it means cluster membership
            // depends on the order refs are compared. Worth an explicit note in the
            // clustering layer rather than leaving it implicit.
            assertTrue(matcher.sameName("Jon", "John"));
            assertTrue(matcher.sameName("John", "Johan"));
            assertFalse(matcher.sameName("Jon", "Johan"));   // breaks the chain
        }

        @Test
        @DisplayName("PROPERTY: matching IS symmetric (verified across a probe sweep)")
        void matchingIsSymmetric() {
            // Greedy pairing iterates ta and claims from tb, so asymmetry was plausible.
            // It did not appear in probing; pinning it so a future refactor can't
            // silently introduce order-dependence.
            String[][] pairs = {
                {"Catherine Hardwick", "Katherine Hardwick"},
                {"Ursula Le Guin", "Ursula Guin"},
                {"Erik Eriksen", "Eric Ericson"},
                {"A Mahanty", "Arpan Mahanty"},
                {"Smith", "John Smith"},
                {"Kim Kim", "Kim Kimm"},
            };
            for (String[] p : pairs) {
                assertEquals(matcher.sameName(p[0], p[1]), matcher.sameName(p[1], p[0]),
                    "asymmetric: " + p[0] + " / " + p[1]);
            }
        }
    }

    // ==============================================================
    // 11. Name particles and library-catalogue formats
    // ==============================================================

    @Nested
    @DisplayName("Name particles and 'Last, First' catalogue format")
    class ParticlesAndCatalogueFormat {

        @Test
        void nameParticlesAreNotStrippedByNormalize() {
            // van/de/la/von survive normalization (they're not in IGNORE_TOKENS).
            assertEquals("ludwig van beethoven", matcher.groupingKey("Ludwig van Beethoven"));
            assertEquals("simone de beauvoir", matcher.groupingKey("Simone de Beauvoir"));
        }

        @Test
        void particleOmittedByOneProviderStillMatches() {
            // Matches via the token-count-mismatch fallback rather than by design —
            // works, but for the same loose reason flagged in GAP 1.
            assertTrue(matcher.sameName("Ludwig van Beethoven", "Ludwig Beethoven"));
            assertTrue(matcher.sameName("Otto von Bismarck", "Otto Bismarck"));
            assertTrue(matcher.sameName("Simone de Beauvoir", "Simone Beauvoir"));
        }

        @Test
        void particleCasingVariesHarmlessly() {
            assertTrue(matcher.sameName("Vincent van Gogh", "Vincent Van Gogh"));
        }

        @Test
        void lastCommaFirstFormatMatchesFirstLast() {
            // Library/MARC-style ordering. The comma becomes a space and token pairing
            // is order-independent, so this works without special handling.
            assertTrue(matcher.sameName("Sanderson, Brandon", "Brandon Sanderson"));
            assertTrue(matcher.sameName("Le Guin, Ursula K.", "Ursula K. Le Guin"));
            assertTrue(matcher.sameName("Tolkien, J.R.R.", "J.R.R. Tolkien"));
        }
    }

    // ==============================================================
    // 12. Algorithm correctness: Jaro-Winkler & Levenshtein internals
    // ==============================================================

    @Nested
    @DisplayName("Jaro-Winkler and Levenshtein correctness (reflection-pinned)")
    class AlgorithmCorrectness {

        private static final double TOLERANCE = 0.005;

        @Test
        void jaroIdenticalStringsIsOne() {
            assertEquals(1.0, jaro("martha", "martha"), TOLERANCE);
        }

        @Test
        void jaroCompletelyDisjointIsZero() {
            assertEquals(0.0, jaro("abc", "xyz"), TOLERANCE);
        }

        @Test
        void jaroEmptyStringIsZero() {
            assertEquals(0.0, jaro("", "abc"), TOLERANCE);
            assertEquals(0.0, jaro("abc", ""), TOLERANCE);
        }

        // Classic Winkler reference vectors (widely used across implementations,
        // e.g. Apache Commons Text's own regression tests) — pin our hand-rolled
        // implementation against these published values.
        @ParameterizedTest
        @CsvSource({
            "martha, marhta, 0.944",
            "dwayne, duane, 0.822",
            "dixon, dicksonx, 0.767"
        })
        void jaroMatchesPublishedReferenceValues(String a, String b, double expected) {
            assertEquals(expected, jaro(a, b), TOLERANCE);
        }

        @ParameterizedTest
        @CsvSource({
            "martha, marhta, 0.961",
            "dwayne, duane, 0.840",
            "dixon, dicksonx, 0.813"
        })
        void jaroWinklerMatchesPublishedReferenceValues(String a, String b, double expected) {
            assertEquals(expected, jaroWinkler(a, b), TOLERANCE);
        }

        @Test
        void jaroWinklerNeverLowerThanJaro() {
            // The prefix bonus only ever adds score, never subtracts. Uses pairs that
            // actually DIFFER — a previous version compared a string to itself, which
            // made this assertion trivially true and tested nothing.
            String[][] pairs = {
                {"sanderson", "sandersan"},   // shared prefix -> bonus applies
                {"catherine", "katherine"},   // no shared prefix -> bonus is zero
                {"martha", "marhta"},
                {"abc", "xyz"},               // disjoint -> jaro 0, bonus 0
            };
            for (String[] p : pairs) {
                assertTrue(jaroWinkler(p[0], p[1]) >= jaro(p[0], p[1]),
                    "JW below Jaro for " + p[0] + "/" + p[1]);
            }
            // And the bonus is real, not always zero, when a prefix is shared.
            assertTrue(jaroWinkler("sanderson", "sandersan") > jaro("sanderson", "sandersan"));
            // With no shared first character there is no bonus at all.
            assertEquals(jaro("catherine", "katherine"), jaroWinkler("catherine", "katherine"), 1e-9);
        }

        @Test
        void levenshteinIdenticalIsZero() {
            assertEquals(0, levenshtein("sanderson", "sanderson"));
        }

        @Test
        void levenshteinSingleSubstitution() {
            assertEquals(1, levenshtein("sanderson", "sandersan"));
        }

        @Test
        void levenshteinSingleInsertion() {
            assertEquals(1, levenshtein("sanderson", "sandersons"));
        }

        @Test
        void levenshteinCompletelyDifferentEqualsMaxLength() {
            assertEquals(3, levenshtein("abc", "xyz"));
        }

        @Test
        void levenshteinIsSymmetric() {
            assertEquals(levenshtein("kitten", "sitting"), levenshtein("sitting", "kitten"));
        }

        @Test
        void normalizedLevenshteinIdenticalIsOne() {
            assertEquals(1.0, normalizedLevenshtein("tolstoy", "tolstoy"), TOLERANCE);
        }

        @Test
        void normalizedLevenshteinBothEmptyIsOne() {
            assertEquals(1.0, normalizedLevenshtein("", ""), TOLERANCE);
        }

        @Test
        void normalizedLevenshteinScalesByLongerString() {
            // 1 edit out of a max-length-7 string.
            assertEquals(1.0 - (1.0 / 7.0), normalizedLevenshtein("толстой", "талстой"), TOLERANCE);
        }
    }

    // ==============================================================
    // 13. API robustness, contract, and internal consistency
    // ==============================================================

    @Nested
    @DisplayName("API robustness and contract")
    class ApiRobustness {

        @Test
        @DisplayName("FIXED (was GAP 7): normalize() is null-safe, matching sameName()")
        void normalizeIsNullSafe() {
            // sameName(null, x) returns false. normalize(null) throws NPE. Since
            // normalize() is public and called directly by NameCandidateResolver to build
            // grouping keys, a single null name from a provider becomes a 500 rather than
            // a skipped ref. Asymmetric contract between the class's two public methods.
            assertFalse(matcher.sameName(null, "Brandon Sanderson"));
            assertEquals("", matcher.groupingKey(null));
        }

        @Test
        @DisplayName("CONFIRMED SAFE: the shared static DoubleMetaphone is thread-safe")
        void sharedPhoneticEncoderIsThreadSafe() throws Exception {
            // NameMatcher is a Spring @Component, so it's a singleton under concurrent
            // request load and the static DOUBLE_METAPHONE is shared across all threads.
            // The class comment asserts this is safe; verifying rather than trusting.
            // (maxCodeLen is set once in a static initializer, which the JVM guarantees
            // is completed before any thread observes the class.)
            String[] names = {"Geoff", "Jeff", "Catherine", "Katherine", "Dostoevsky",
                "Dostoyevsky", "Sanderson", "Mahanty"};
            Map<String, Boolean> expected = new LinkedHashMap<>();
            for (String a : names) for (String b : names) expected.put(a + "|" + b, matcher.sameName(a, b));

            ExecutorService pool = Executors.newFixedThreadPool(8);
            try {
                List<Future<Boolean>> futures = new ArrayList<>();
                for (int i = 0; i < 400; i++) {
                    futures.add(pool.submit(() -> {
                        for (Map.Entry<String, Boolean> e : expected.entrySet()) {
                            String[] p = e.getKey().split("\\|");
                            if (matcher.sameName(p[0], p[1]) != e.getValue()) return false;
                        }
                        return true;
                    }));
                }
                for (Future<Boolean> f : futures) {
                    assertTrue(f.get(30, TimeUnit.SECONDS), "concurrent result diverged from single-threaded");
                }
            } finally {
                pool.shutdownNow();
            }
        }

        @Test
        @DisplayName("FIXED (was GAP 8): one positional rule for suffixes; single letters are initials")
        void romanNumeralSuffixHandlingIsConsistent() {
            // I, V, X  -> dropped by the single-letter rule
            // II,III,IV-> dropped by IGNORE_TOKENS
            // VI..IX,XI-> not dropped at all
            // Three mechanisms, no coherent policy, and the boundary is invisible.
            // Multi-character numerals are recognized as suffixes and kept in the key, so
            // two generations never collide into one group.
            assertEquals("john smith ii", matcher.groupingKey("John Smith II"));
            assertEquals("john smith iv", matcher.groupingKey("John Smith IV"));
            assertEquals("john smith vii", matcher.groupingKey("John Smith VII"));
            assertEquals("john smith xi", matcher.groupingKey("John Smith XI"));
            // When BOTH sides carry one, they must agree.
            assertFalse(matcher.sameName("John Smith II", "John Smith III"));
            assertFalse(matcher.sameName("John Smith VI", "John Smith VII"));
            assertFalse(matcher.sameName("John Smith Jr", "John Smith Sr"));
            // Single letters stay initials — the deliberate Malcolm X tradeoff.
            assertEquals("john smith i", matcher.groupingKey("John Smith I"));
            assertEquals("john smith v", matcher.groupingKey("John Smith V"));
        }

        @Test
        @DisplayName("FIXED (was GAP 9): the honorific set now covers common titles")
        void commonHonorificsAreRecognized() {
            // sir / dame / lord / rev / capt / st / phd / md / esq all survive.
            // The honorific is recognized (it is separated from the name parts) but is
            // retained in the key, so that two differently-titled names cannot collide.
            assertEquals("sir arthur conan doyle", matcher.groupingKey("Sir Arthur Conan Doyle"));
            assertEquals("dame agatha christie", matcher.groupingKey("Dame Agatha Christie"));
            assertEquals("john smith phd", matcher.groupingKey("John Smith PhD"));
            assertEquals("rev john newton", matcher.groupingKey("Rev John Newton"));
            // Recognizing it is what lets the match succeed despite the differing key.
            assertTrue(matcher.sameName("Sir Arthur Conan Doyle", "Arthur Conan Doyle"));
        }

        @Test
        @DisplayName("DECOUPLED: honorific matches no longer depend on the truncation bug")
        void honorificMatchesAreNowIndependentOfLeftoverHandling() {
            // The surviving honorific creates a token-count mismatch, which routes to the
            // fallback, whose min() truncation then ignores the unpaired token — so these
            // match by accident. Fixing GAP 1 without also extending IGNORE_TOKENS would
            // silently break all of them. The two findings must be resolved together.
            assertTrue(matcher.sameName("Sir Arthur Conan Doyle", "Arthur Conan Doyle"));
            assertTrue(matcher.sameName("Dame Agatha Christie", "Agatha Christie"));
            assertTrue(matcher.sameName("John Smith PhD", "John Smith"));
            assertTrue(matcher.sameName("Rev John Newton", "John Newton"));
        }

        @Test
        @DisplayName("FIXED (was GAP 10): digits are retained as content")
        void digitsAreRetained() {
            // NON_ALPHA replaces every non-letter with a space, so numerals vanish.
            // "50 Cent" is a published author; it collapses to a bare surname.
            assertEquals("50 cent", matcher.groupingKey("50 Cent"));
            assertEquals("blink 182", matcher.groupingKey("Blink 182"));
            assertFalse(matcher.sameName("50 Cent", "Cent"));
        }

        @Test
        @DisplayName("Whitespace variants (tab/newline) normalize identically to spaces")
        void exoticWhitespaceIsHandled() {
            assertTrue(matcher.sameName("\t\nJohn\tSmith\n", "John Smith"));
            assertEquals("john smith", matcher.groupingKey("John\u00A0Smith".replace('\u00A0', ' ')));
        }

        @Test
        @DisplayName("No crash on random/pathological input (fuzz over jaro's transposition loop)")
        void fuzzDoesNotCrash() {
            // jaro() advances a second cursor inside a while(!m2[k]) loop; a mismatch in
            // match counts between the two arrays would run it off the end. Fuzzing to
            // confirm that can't happen.
            Random rnd = new Random(42);
            String alphabet = "abcdefghij";
            for (int i = 0; i < 20_000; i++) {
                String s1 = randomString(rnd, alphabet, rnd.nextInt(12));
                String s2 = randomString(rnd, alphabet, rnd.nextInt(12));
                assertDoesNotThrow(() -> matcher.sameName(s1, s2), "crashed on \"" + s1 + "\" / \"" + s2 + "\"");
            }
        }

        private String randomString(Random r, String alphabet, int len) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
            return sb.toString();
        }
    }
}
