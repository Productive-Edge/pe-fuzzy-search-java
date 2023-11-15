package com.pe.text;

/**
 * Interface of the successfully matched result.
 * <p>
 * {@link FuzzyMatcher} itself implements this interface to reduce heap memory usage
 * when search is done via {@link FuzzyMatcher#find()} method in the {@code while} loop:
 * <pre>{@code
 *     FuzzyMatcher matcher = pattern.matcher(text);
 *     while(matcher.find()) {
 *      System.out.println("Found text: " + text.substring(matcher.start(), matcher.end());
 *     }
 * }</pre>
 * <p>
 * Internal class {@link FuzzyResultRecord} also implements this interface to store matching results produced by
 * {@link FuzzyMatcher#stream()} and {@link FuzzyMatcher#findTheBestMatching()}
 */
public interface FuzzyResult {

    /**
     * Returns the start index of the match.
     *
     * @return The index of the first character matched.
     * @throws IllegalStateException in case {@link FuzzyMatcher#start()} was called when no current matching was found.
     */
    int start();

    /**
     * Returns the offset after the last character matched.
     * <pre>{@code
     *     FuzzyMatcher matcher = pattern.matcher(text);
     *     while(matcher.find()) {
     *      System.out.println("Found text: " + text.substring(matcher.start(), matcher.end());
     *     }
     * }</pre>
     *
     * @return The offset after the last character matched.
     * @throws IllegalStateException in case {@link FuzzyMatcher#end()} was called when no current matching was found.
     */
    int end();

    /**
     * Returns the found subsequence in the input text.
     * For a matcher <code>m</code> with input sequence <code>s</code>, the expressions <code>m.foundText()</code> and <code>s.subSequence(m.start(), m.end())</code> are equivalent.
     *
     * @return The found subsequence in the input text.
     * @throws IllegalStateException in case {@link FuzzyMatcher#foundText()} was called when no current matching was found.
     */
    CharSequence foundText();

    /**
     * Returns similarity of the found subsequence with the pattern, which value is between 0.0f and 1.0f calculated as
     * {@code (pattern.length - distance) / pattern.length}
     *
     * @return similarity of the found subsequence with the pattern, which value is between 0.0f and 1.0f
     */
    default float similarity() {
        return pattern().text().length() - distance() / (float) pattern().text().length();
    }

    /**
     * The reference of the fuzzy search pattern was successfully matched.
     *
     * @return The reference of the fuzzy search pattern.
     */
    FuzzyPattern pattern();

    /**
     * Returns Levenshtein distance between the pattern text and found one.
     *
     * @return The Levenshtein distance between the pattern text and found one.
     * @throws IllegalStateException in case {@link FuzzyMatcher#distance()} was called when no current matching was found.
     */
    int distance();
}
