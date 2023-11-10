package com.pe.text;

/**
 * Common interface for the simple {@link FuzzyPattern} and multiple pattern instance created
 * via {@link FuzzyMultiPattern#combine(FuzzyPattern, FuzzyPattern, FuzzyPattern...)}
 */
interface MatcherProvider {

    /**
     * Creates {@link FuzzyMatcher} for the specified text. Equivalent to the {@code pattern.matcher(text, 0, text.length())}
     *
     * @param text The text to scan.
     *
     * @return {@link FuzzyMatcher} instance with initial state.
     */
    default FuzzyMatcher matcher(CharSequence text) {
        return matcher(text, 0, text.length());
    }

    /**
     * Creates {@link FuzzyMatcher} for the specified text, and start offset. Equivalent to the {@code paatern.matcher(text, fromIndex, text.length())}
     *
     * @param text The text to scan.
     * @param fromIndex The start offset to scan.
     *
     * @return {@link FuzzyMatcher} instance with initial state, where index to start scanning is specified.
     */
    default FuzzyMatcher matcher(CharSequence text, int fromIndex) {
        return matcher(text, fromIndex, text.length());
    }

    /**
     * Creates {@link FuzzyMatcher} for the specified text, and defined range to scan.
     *
     * @param text The text to scan.
     * @param fromIndex The start offset to scan.
     * @param toIndex The end offset to stop further scanning
     * @return {@link FuzzyMatcher} instance with initial state, where scanning bounds are specified.
     */
    FuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex);
}
