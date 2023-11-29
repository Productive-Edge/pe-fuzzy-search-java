package com.pe.text;

/**
 * Fuzzy pattern interface. Instance can be created via {@link FuzzyPattern#compile(CharSequence, int)}
 * and {@link FuzzyPattern#compile(CharSequence, int, boolean)}
 */
public interface FuzzyPattern extends MatcherProvider {

    /**
     * Creates case-sensitive compiled fuzzy search pattern with maximum allowed Levenshtein distance to match.
     *
     * @param pattern                Text of the pattern
     * @param maxLevenshteinDistance Maximum allowed Levenshtein distance to match
     * @return Case-sensitive compiled fuzzy search pattern.
     * @throws IllegalArgumentException if specified text is null or empty
     */
    static FuzzyPattern compile(CharSequence pattern, int maxLevenshteinDistance) {
        return compile(pattern, maxLevenshteinDistance, false);
    }

    /**
     * Creates compiled fuzzy search pattern with maximum allowed Levenshtein distance and specified case-sensitivity to match.
     *
     * @param pattern                Text of the pattern
     * @param maxLevenshteinDistance Maximum allowed Levenshtein distance to match
     * @param caseInsensitive        Case-insensitivity for the pattern.
     *                               if {@code true} - the pattern's matcher will ignore casing when scanning.
     * @return Compiled fuzzy search pattern with specified case-sensitivity.
     * @throws IllegalArgumentException if specified text is null or empty
     */
    static FuzzyPattern compile(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        if (pattern == null)
            throw new IllegalArgumentException("pattern text can not be null");
        if (pattern.length() == 0)
            throw new IllegalArgumentException("pattern text can not be empty");
        if (pattern.length() <= 32)
            return new Bitap32(pattern, maxLevenshteinDistance, caseInsensitive);
        if (pattern.length() <= 64)
            return new Bitap64(pattern, maxLevenshteinDistance, caseInsensitive);
        return new Bitap65Plus(pattern, maxLevenshteinDistance, caseInsensitive);
    }

    /**
     * Returns text of this pattern.
     *
     * @return Text of this pattern.
     */
    CharSequence text();

    /**
     * Returns maximal allowed Levenshtein distance (i.e. count of character insertions, deletions, or replacements) for found matchings.
     *
     * @return The maximal allowed Levenshtein distance (i.e. count of character insertions, deletions, or replacements) for found matchings.
     */
    int maxLevenshteinDistance();

    /**
     * Indicates case sensitivity of this pattern
     *
     * @return {@code false} if pattern is case-sensitive (by default pattern is case-sensitive), otherwise - {@code true}
     */
    boolean caseInsensitive();

}