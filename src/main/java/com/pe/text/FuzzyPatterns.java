package com.pe.text;

import com.pe.ordinal.OrdinalSuffix;

/**
 * A specialized interface for combining multiple fuzzy patterns into a single pattern.
 * <p>
 * FuzzyPatterns allows you to search for multiple patterns in a single pass through the text,
 * returning non-overlapping matches in the order they appear in the text. This is particularly
 * useful when you need to search for multiple patterns in the same text and want to process
 * the results in a unified way.
 *
 * <h2>Use Cases</h2>
 *
 * <h3>1. Finding the first occurrence of any pattern</h3>
 * <p>
 * This combined pattern is efficient when you only need one or a few matches:
 *
 * <pre>{@code
 *   // Create a combined pattern to detect unwanted ingredients
 *   private static final FuzzyPatterns INGREDIENTS_TO_EXCLUDE = FuzzyPatterns.combine(
 *      FuzzyPattern.compile("Corn Syrup", 3, true), // maximum 3 OCR errors, case insensitive
 *      FuzzyPattern.compile("Tomato Concentrate", 4, true) // maximum 4 OCR errors, case insensitive
 *   );
 *
 *   // Check if a product contains any unwanted ingredient
 *   public static boolean hasUnwantedIngredientIn(String ketchupIngredientsWithOcrErrors) {
 *       return INGREDIENTS_TO_EXCLUDE.matcher(ketchupIngredientsWithOcrErrors).find();
 *   }
 * }</pre>
 *
 * <h3>2. Alternative approach for finding all matches</h3>
 * <p>
 * For cases where all matches must be found and order is not important,
 * it can be more efficient to process each pattern separately:
 *
 * <pre>{@code
 *     // Define an array of patterns to search for
 *     private static final FuzzyPattern[] KEYWORDS = {
 *         FuzzyPattern.compile("56a. Provider", 3),
 *         FuzzyPattern.compile("Speciality Code", 4),
 *         FuzzyPattern.compile("57. Phone", 3),
 *         FuzzyPattern.compile("52. Phone", 3),
 *         FuzzyPattern.compile("49. NPI", 2),
 *         FuzzyPattern.compile("50. License Number", 5),
 *         FuzzyPattern.compile("57. License Number", 5),
 *         FuzzyPattern.compile("51. SSN or TIN", 3),
 *         // Address components
 *         FuzzyPattern.compile("Street", 2, true),
 *         FuzzyPattern.compile("Suite", 2, true),
 *         FuzzyPattern.compile("Floor", 2, true),
 *         FuzzyPattern.compile("Drive", 2, true)
 *     };
 *
 *     public void process(Document document) {
 *         final String text = document.getText();
 *         final int startFrom = (text.length() * 3) / 4;
 *
 *         // Process each pattern separately for better performance when order doesn't matter
 *         Arrays.stream(KEYWORDS)
 *             .flatMap(pattern -> pattern.matcher(text, startFrom).stream())
 *             .forEach(result ->
 *                 document.add(
 *                     NamedEntity.descriptor()
 *                         .setType("W56_" + result.pattern().text())
 *                         .setScore(1)
 *                         .setBegin(result.start())
 *                         .setEnd(result.end()))
 *             );
 *     }
 * }</pre>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *     <li>Use {@code FuzzyPatterns} when you need to find matches in order of appearance in the text</li>
 *     <li>Use {@code FuzzyPatterns} when you need to ensure non-overlapping matches</li>
 *     <li>For finding all matches where order doesn't matter, processing patterns individually may be faster</li>
 * </ul>
 *
 * <h2>Edge Cases</h2>
 * <ul>
 *     <li>If patterns would produce overlapping matches, only the first match (by position in text) is returned</li>
 *     <li>If multiple patterns match at the same position, the one with the lowest Levenshtein distance is preferred</li>
 *     <li>If multiple patterns match at the same position with the same distance, the pattern that was added first is preferred</li>
 * </ul>
 *
 * @see FuzzyPattern
 * @see FuzzyMatcher
 * @see FuzzyMatcherProvider
 */
public interface FuzzyPatterns extends FuzzyMatcherProvider {

    /**
     * Creates an instance of {@code FuzzyPatterns} by combining multiple fuzzy patterns.
     * <p>
     * This method combines two or more patterns into a single pattern that can match any of the
     * input patterns. When searching with the resulting pattern, matches will be returned in order
     * of their appearance in the text, regardless of which original pattern produced the match.
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     *     // Combine patterns for different name formats
     *     FuzzyPatterns namePatterns = FuzzyPatterns.combine(
     *         FuzzyPattern.compile("John Smith", 2),
     *         FuzzyPattern.compile("Smith, John", 2),
     *         FuzzyPattern.compile("J. Smith", 1)
     *     );
     *
     *     // Search for any name format in the text
     *     FuzzyMatcher matcher = namePatterns.matcher(document);
     *     while (matcher.find()) {
     *         System.out.println("Found name: " + matcher.foundText());
     *     }
     * }</pre>
     *
     * <p><strong>Edge Cases:</strong>
     * <ul>
     *     <li>If any of the patterns is null, a NullPointerException will be thrown</li>
     *     <li>If patterns would produce overlapping matches, only the first match (by position) is returned</li>
     *     <li>If multiple patterns match at the same position, the one with the lowest Levenshtein distance is preferred</li>
     * </ul>
     *
     * @param first  The first fuzzy pattern to combine.
     * @param second The second fuzzy pattern to combine.
     * @param others Optional additional fuzzy patterns to combine.
     * @return A combined pattern that can match any of the input patterns in a single scan.
     * @throws NullPointerException if any of the arguments is null.
     * @see #patterns()
     */
    static FuzzyPatterns combine(FuzzyMatcherProvider first, FuzzyMatcherProvider second, FuzzyMatcherProvider... others) {
        if (first == null)
            throw new NullPointerException("1st pattern is null");
        if (second == null)
            throw new NullPointerException("2nd pattern is null");
        for (int i = 0; i < others.length; i++)
            if (others[i] == null)
                throw new NullPointerException(OrdinalSuffix.EN.addTo(i + 1) + " pattern is null");

        return first.combineWith(second, others);
    }

    /**
     * Returns an iterable collection of all the fuzzy patterns that were combined in this instance.
     * <p>
     * This method provides access to the individual patterns that make up this combined pattern.
     * This can be useful for inspecting which patterns are included or for accessing properties
     * of the individual patterns.
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     *     FuzzyPatterns combined = FuzzyPatterns.combine(
     *         FuzzyPattern.compile("apple", 1),
     *         FuzzyPattern.compile("banana", 1),
     *         FuzzyPattern.compile("cherry", 1)
     *     );
     *
     *     // Iterate through the individual patterns
     *     for (FuzzyMatcherProvider pattern : combined.patterns()) {
     *         if (pattern instanceof FuzzyPattern) {
     *             FuzzyPattern fp = (FuzzyPattern) pattern;
     *             System.out.println("Pattern: " + fp.text() +
     *                               ", Max distance: " + fp.maxLevenshteinDistance());
     *         }
     *     }
     * }</pre>
     *
     * @return An iterable collection of the individual fuzzy patterns that make up this combined pattern.
     * @see #combine(FuzzyMatcherProvider, FuzzyMatcherProvider, FuzzyMatcherProvider...)
     */
    Iterable<? extends FuzzyMatcherProvider> patterns();
}
