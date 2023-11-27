package com.pe.text;

/**
 * Internal interface which returns {@link IterativeFuzzyMatcher}
 */
interface IterativeFuzzyPattern extends MatcherProvider {
    default FuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex) {
        return getIterativeMatcher(text, fromIndex, toIndex);
    }

    /**
     * Same as {@link FuzzyPattern#matcher(CharSequence, int, int)} but returns {@link IterativeFuzzyMatcher}
     *
     * @param text      The text to scan.
     * @param fromIndex The start offset to scan.
     * @param toIndex   The end offset to stop further scanning
     * @return {@link IterativeFuzzyMatcher}
     * @see FuzzyPattern#matcher(CharSequence, int, int)
     */
    IterativeFuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex);

}