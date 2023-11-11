package com.pe.text;

/**
 * Internal interface which returns {@link IterativeFuzzyMatcher}
 */
interface IterativeFuzzyPattern extends MatcherProvider {
    default FuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex) {
        return getIterativeMatcher(text, fromIndex, toIndex);
    }

    IterativeFuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex);

}