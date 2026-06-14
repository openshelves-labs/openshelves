package com.openshelves.services.metadata.resolver;

import com.openshelves.model.dto.metadata.FieldResolution;
import com.openshelves.model.entity.FieldResolutionPolicyEntity;
import com.openshelves.model.enums.MetadataField;
import com.openshelves.model.enums.MetadataProvider;
import com.openshelves.model.enums.ResolutionStrategy;
import com.openshelves.services.metadata.MetadataFieldResolver;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/// Resolves metadata fields — specifically textual ones like descriptions, abstracts,
/// or notes — by evaluating multiple candidates and selecting the one with the highest
/// calculated quality score.
///
/// The quality calculation uses a weighted combination of multiple heuristic metrics:
///
///   - **Text Length:** Prefers values within a standard length range, penalizing
///     very short or extremely long text.
///
///   - **Word Density:** Assesses the ratio of meaningful content words versus filler
///     or stop words. Penalizes keyword-stuffed lists or content lacking substance.
///
///   - **Sentence Coherence:** Evaluates basic syntactic markers (capitalization,
///     ending punctuation) and penalizes excessively choppy or run-on sentences.
///
///   - **Structural Integrity:** Flags indicators of poor extraction, such as HTML
///     tags, unescaped HTML entities, trailing truncation markers (e.g., ellipses),
///     junk/control characters, and repeated punctuation.
///
/// If candidates score too closely to each other (within [TIE_TOLERANCE_BAND]), the
/// resolver flags the field for human verification rather than making an arbitrary
/// choice.
@Component
public class HighestQualityTextResolver implements MetadataFieldResolver {

    // ----------------------------------------------------------------
    // Resolution Thresholds & Settings
    // ----------------------------------------------------------------

    // Scores below this threshold disqualify a candidate entirely from being
    // auto-resolved.
    public static final double MINIMUM_QUALITY_THRESHOLD = 0.25;

    // Score differences smaller than this between the top two candidates are
    // treated as a tie.
    public static final double TIE_TOLERANCE_BAND = 0.05;

    // ----------------------------------------------------------------
    // Quality Heuristic Weights (must sum to 1.0)
    // ----------------------------------------------------------------

    // Weight assigned to the text length metric.
    private static final double TEXT_LENGTH_SCORE_WEIGHT = 0.35;

    // Weight assigned to the word density metric.
    private static final double WORD_DENSITY_SCORE_WEIGHT = 0.35;

    // Weight assigned to the sentence coherence metric.
    private static final double SENTENCE_COHERENCE_SCORE_WEIGHT = 0.20;

    // Weight assigned to the structural integrity metric.
    private static final double STRUCTURAL_INTEGRITY_SCORE_WEIGHT = 0.10;

    // ----------------------------------------------------------------
    // Metric-Specific Constants & Limits
    // ----------------------------------------------------------------

    // Minimum text length in characters below which linear penalties apply.
    private static final int MIN_TEXT_LENGTH = 80;

    // Maximum text length in characters above which logarithmic decay penalties
    // apply.
    private static final int MAX_TEXT_LENGTH = 1500;

    // ----------------------------------------------------------------
    // Heuristic Patterns: Structural Integrity
    // ----------------------------------------------------------------

    // Matches HTML tag markers (e.g., <div>, </p>).
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    // Matches common HTML entities (e.g., &amp;, &#123;).
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&[a-z]+;|&#\\d+;");

    // Matches trailing truncation symbols (e.g., ..., …) at the end of the text.
    private static final Pattern TRUNCATION_PATTERN = Pattern.compile("(\\.{3,}|…)\\s*$");

    // Matches control or junk characters (non-printable ASCII / control codes).
    private static final Pattern JUNK_CHARS_PATTERN = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    // Matches excessive repeated punctuation (e.g., !!!, ???).
    private static final Pattern REPEATED_PUNCTUATION_PATTERN = Pattern.compile("[!?,;:]{3,}");

    // ----------------------------------------------------------------
    // Heuristic Patterns: Sentence Coherence
    // ----------------------------------------------------------------

    // Splits text into individual sentences based on trailing punctuation.
    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("(?<=[.!?])\\s+");

    // Checks if a sentence ends with proper terminating punctuation (period,
    // question mark, or exclamation mark).
    private static final Pattern TERMINATES_WITH_PUNCTUATION_PATTERN = Pattern.compile(".*[.!?]$");

    // ----------------------------------------------------------------
    // Stop Words Reference Data
    // ----------------------------------------------------------------

    // Set of English stop words loaded from the resource classpath.
    private static final Set<String> STOP_WORDS;

    static {
        // Load a standard list of English stop words (could be expanded or made
        // configurable)
        try (InputStream is = HighestQualityTextResolver.class.getResourceAsStream("/metadata/stop_words.txt")) {

            if (is == null) {
                throw new IllegalStateException("Failed to load stop words: Resource not found");
            }

            // Read the stream line-by-line and collect all the stop words
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                STOP_WORDS = reader.lines().map(String::strip)
                        .filter(line -> !line.isBlank() && !line.startsWith("#")) // Ignore blank lines and comments
                        .collect(Collectors.toUnmodifiableSet());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load stop words", e);
        }
    }

    @Override
    public ResolutionStrategy strategy() {
        return ResolutionStrategy.HIGHEST_QUALITY_TEXT;
    }

    /// Resolves the best candidate value for a metadata field based on text quality
    /// heuristics.
    ///
    /// Deduplicates and scores candidates, checks for quality thresholds, and flags ties.
    ///
    /// @param field      the metadata field being resolved (e.g., DESCRIPTION)
    /// @param candidates a map of metadata providers to their corresponding candidate values
    /// @param policy     the resolution policy configuration
    /// @param <T>        the type of the candidate values
    ///
    /// @return a [FieldResolution] indicating the resolution result (Absent,
    ///         Resolved, NeedsConfirmation, or Blocked)
    @Override
    public <T> FieldResolution<T> resolve(MetadataField field, Map<MetadataProvider, T> candidates, FieldResolutionPolicyEntity policy) {

        // Extract, clean, and deduplicate upfront
        Set<T> uniqueCandidates = candidates.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (uniqueCandidates.isEmpty()) {
            return new FieldResolution.Absent<>();
        }

        // Short-circuit: If there's only one unique text block, just check for quality
        if (uniqueCandidates.size() == 1) {
            T candidate = uniqueCandidates.iterator().next();
            double qualityScore = calculateTextQuality(candidate.toString());

            if (qualityScore >= MINIMUM_QUALITY_THRESHOLD) {
                return new FieldResolution.Resolved<>(candidate);
            } else {
                return new FieldResolution.NeedsConfirmation<>(candidate);
            }
        }

        // Score the unique candidates and order them by quality (highest -> lowest)
        List<ScoredCandidate<T>> scoredCandidates = uniqueCandidates.stream()
                .map(candidate -> new ScoredCandidate<>(candidate, calculateTextQuality(candidate.toString())))
                .sorted((a, b) -> Double.compare(b.getQualityScore(), a.getQualityScore())) // Descending order
                .toList();

        // Evaluate conflict threshold between top 2 candidates
        ScoredCandidate<T> bestCandidate = scoredCandidates.get(0);
        ScoredCandidate<T> secondBestCandidate = scoredCandidates.get(1);

        double diff = bestCandidate.getQualityScore() - secondBestCandidate.getQualityScore();
        if (diff < TIE_TOLERANCE_BAND) {
            // Too close to call — flag for human review
            return new FieldResolution.Blocked<>(candidates);
        }

        // Check if the best candidate meets the minimum quality threshold
        if (bestCandidate.qualityScore >= MINIMUM_QUALITY_THRESHOLD) {
            return new FieldResolution.Resolved<>(bestCandidate.getCandidate());
        } else {
            return new FieldResolution.NeedsConfirmation<>(bestCandidate.getCandidate());
        }
    }

    /// Evaluates a text block and returns a normalized quality score between 0.0 and 1.0.
    /// A score of 1.0 represents a clean, well-formatted, and cohesive block of prose,
    /// while 0.0 represents empty or extremely low-quality content.
    ///
    /// @param text the candidate text to score
    /// @return a quality score in the range [0.0, 1.0]
    private double calculateTextQuality(String text) {
        if (StringUtils.isBlank(text)) {
            return 0;
        }

        String strippedText = text.strip();

        double textLengthScore = scoreTextLength(strippedText);
        double wordDensityScore = scoreWordDensity(strippedText);
        double sentenceCoherenceScore = scoreSentenceCoherence(strippedText);
        double structuralIntegrityScore = scoreStructuralIntegrity(strippedText);

        // Combine the individual scores into an overall quality score (weighted
        // average)
        double qualityScore = TEXT_LENGTH_SCORE_WEIGHT * textLengthScore
                + WORD_DENSITY_SCORE_WEIGHT * wordDensityScore
                + SENTENCE_COHERENCE_SCORE_WEIGHT * sentenceCoherenceScore
                + STRUCTURAL_INTEGRITY_SCORE_WEIGHT * structuralIntegrityScore;

        // Guardrail to ensure the final score is between 0 and 1
        return Math.clamp(qualityScore, 0, 1);
    }

    /// Scores the text based on its length in characters.
    ///
    /// Prefers text within the range [[MIN_TEXT_LENGTH], [MAX_TEXT_LENGTH]]:
    ///
    ///   - Texts shorter than [MIN_TEXT_LENGTH] are penalized linearly.
    ///
    ///   - Texts within the range get a perfect score of 1.0.
    ///
    ///   - Texts longer than [MAX_TEXT_LENGTH] are penalized logarithmically to avoid
    ///     harshly penalizing slightly longer, detailed descriptions.
    ///
    /// @param text the text to score
    /// @return a length score in the range [0.0, 1.0]
    private double scoreTextLength(String text) {
        int textLength = text.length();
        if (textLength == 0) {
            return 0;
        }

        if (textLength < MIN_TEXT_LENGTH) {
            return (double) textLength / MIN_TEXT_LENGTH; // Linear penalty for being too short
        }

        if (textLength > MAX_TEXT_LENGTH) {
            // Logarithmic decay to avoid overly harsh penalties for slightly long texts
            int excessLength = textLength - MAX_TEXT_LENGTH;
            double penaltyPercentage = Math.log1p((double) excessLength / MAX_TEXT_LENGTH) / Math.log(10);
            return Math.max(0.5, 1 - 0.5 * penaltyPercentage);
        }

        return 1.0; // Perfect score for being within the ideal length range
    }

    /// Scores the text based on the density of meaningful content words (non-stop words).
    ///
    ///   - If the word count is very low (fewer than 3 words), a minimal fractional score is returned.
    ///
    ///   - If the density is between 50% and 85%, it receives a perfect score of 1.0.
    ///
    ///   - If the density is under 50%, it indicates high usage of stop/filler words,
    ///     leading to a linear penalty.
    ///
    ///   - If the density is over 85%, it indicates a high concentration of rare/content
    ///     words — typical of a keyword tag list rather than natural prose — leading to
    ///     a penalty.
    ///
    /// @param text the text to score
    /// @return a density score in the range [0.0, 1.0]
    private double scoreWordDensity(String text) {
        String[] words = text.split("\\s+");
        int wordCount = words.length;

        if (wordCount < 3) {
            // Very short text, can't access word density meaningfully
            return 0.1 * wordCount; // 0.1 for 1 word, 0.2 for 2 words
        }

        long meaningfulWords = Arrays.stream(words)
                .map(w -> w.replaceAll("[^a-zA-Z]", "")) // Remove punctuation
                .filter(w -> w.length() > 1) // Remove short words
                .filter(w -> !STOP_WORDS.contains(w.toLowerCase(Locale.ROOT))) // Remove stop words
                .count();

        double density = (double) meaningfulWords / wordCount;

        if (density < 0.5) {
            // Filler Heavy -> Liner Spread from 0 to 1 as density goes from 0 to .5
            return density * 2;
        }

        if (density < 0.85) {
            // Ideal condition
            return 1;
        }

        // Slight penalty: possibly a keyword list rather than prose
        double excessDensity = density - 0.85;
        double penaltyPercentage = excessDensity / 0.15;
        return 1 - 0.3 * penaltyPercentage;
    }

    /// Scores the text based on sentence structure coherence.
    ///
    /// Evaluates whether sentences begin with a capital letter and end with appropriate
    /// punctuation. Also applies penalties if the average sentence length is too short
    /// (choppy fragments) or too long (run-on sentences).
    ///
    /// @param text the text to score
    /// @return a coherence score in the range [0.0, 1.0]
    private double scoreSentenceCoherence(String text) {
        String[] sentences = SENTENCE_SPLIT_PATTERN.split(text);

        if (sentences.length == 0) {
            return 0;
        }

        if (sentences.length == 1) {
            // Short single phrases are fine; reward terminating punctuation
            String sentence = sentences[0];

            boolean terminatedCorrectly = TERMINATES_WITH_PUNCTUATION_PATTERN.matcher(sentence).matches();
            boolean firstLetterCapitalized = Character.isUpperCase(sentence.charAt(0));

            return 0.5 + (terminatedCorrectly ? 1 : 0) * 0.25 + (firstLetterCapitalized ? 1 : 0) * 0.25;
        }

        // Scan all the sentences to assess overall coherence
        double totalFormatingScore = 0;
        int totalWordCount = 0;

        for (String sentence : sentences) {

            if (StringUtils.isBlank(sentence))
                continue; // Skip blank statements

            sentence = sentence.strip();
            int wordCount = sentence.split("\\s+").length;
            totalWordCount += wordCount;

            boolean terminatedCorrectly = TERMINATES_WITH_PUNCTUATION_PATTERN.matcher(sentence).matches();
            boolean firstLetterCapitalized = Character.isUpperCase(sentence.charAt(0));

            totalFormatingScore += (terminatedCorrectly ? 1 : 0) * 0.5 + (firstLetterCapitalized ? 1 : 0) * 0.5;
        }

        double coherenceRatio = totalFormatingScore / sentences.length;
        double avgSentenceLength = (double) totalWordCount / sentences.length;

        // Penalize run-on sentences (avg > 40 words) or choppy fragments (avg < 5)
        double lengthPenalty = 0;
        if (avgSentenceLength < 8) {
            lengthPenalty = 0.2 * (1 - avgSentenceLength / 8); // Up to 20% penalty for very short sentences
        } else if (avgSentenceLength > 40) {
            lengthPenalty = 0.2 * Math.min(1.0, (avgSentenceLength - 40) / 40); // Up to 20% penalty for very long sentences
        }

        return Math.clamp(coherenceRatio - lengthPenalty, 0, 1);
    }

    /// Assesses the structural cleanliness of the text, penalizing artifacts of poor extraction or formatting.
    ///
    /// Deducts points for:
    ///   - HTML tags — deducts 0.4 points
    ///   - Unescaped HTML entities — deducts 0.15 points
    ///   - Truncation markers like trailing ellipses — deducts 0.5 points
    ///   - Junk/control characters — deducts 0.3 points
    ///   - Repeated punctuation like `!!!` or `???` — deducts 0.2 points
    ///
    /// @param text the text to score
    /// @return a structural integrity score in the range [0.0, 1.0]
    private double scoreStructuralIntegrity(String text) {
        double integrityScore = 1; // Perfect Score

        if (HTML_TAG_PATTERN.matcher(text).find()) {
            integrityScore -= 0.4;
        }

        if (HTML_ENTITY_PATTERN.matcher(text).find()) {
            integrityScore -= 0.15;
        }

        if (TRUNCATION_PATTERN.matcher(text).find()) {
            integrityScore -= 0.5;
        }

        if (JUNK_CHARS_PATTERN.matcher(text).find()) {
            integrityScore -= 0.3;
        }

        if (REPEATED_PUNCTUATION_PATTERN.matcher(text).find()) {
            integrityScore -= 0.2;
        }

        return Math.clamp(integrityScore, 0, 1);
    }

    /// Pairs a candidate value with its calculated quality score.
    @Value
    private static class ScoredCandidate<T> {
        T candidate;
        double qualityScore;
    }
}
