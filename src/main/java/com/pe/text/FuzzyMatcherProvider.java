package com.pe.text;

import com.pe.ordinal.OrdinalSuffix;

import java.util.Arrays;

/**
 * Common interface for the simple {@link FuzzyPattern} and multiple pattern instance {@link FuzzyPatterns}
 */
public interface FuzzyMatcherProvider {

    /**
     * Creates {@link FuzzyMatcher} for the specified text. Equivalent to the {@code pattern.matcher(text, 0, text.length())}
     *
     * @param text The text to scan.
     * @return {@link FuzzyMatcher} instance with initial state.
     */
    default FuzzyMatcher matcher(CharSequence text) {
        return matcher(text, 0, text.length());
    }

    /**
     * Creates {@link FuzzyMatcher} for the specified text, and defined range to scan.
     *
     * @param text      The text to scan.
     * @param fromIndex The start offset to scan.
     * @param toIndex   The end offset to stop further scanning
     * @return {@link FuzzyMatcher} instance with initial state with specified bounds of search.
     */
    FuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex);

    /**
     * Creates {@link FuzzyMatcher} for the specified text, and start offset. Equivalent to the {@code paatern.matcher(text, fromIndex, text.length())}
     *
     * @param text      The text to scan.
     * @param fromIndex The start offset to scan.
     * @return {@link FuzzyMatcher} instance with initial state with specified start index of search
     */
    default FuzzyMatcher matcher(CharSequence text, int fromIndex) {
        return matcher(text, fromIndex, text.length());
    }

    /**
     * Creates instance of the {@code FuzzyPatterns} combining this and specified fuzzy patterns.
     *
     * @param pattern          2nd fuzzy pattern to combine with
     * @param andOtherPatterns optional other patterns to combine with
     * @return instance of the {@code FuzzyPatterns} combining this and specified fuzzy patterns
     * @throws NullPointerException in case any of arguments is null
     */
    default FuzzyPatterns combineWith(FuzzyMatcherProvider pattern, FuzzyMatcherProvider... andOtherPatterns) {
        FuzzyMatcherProvider[] patterns = new FuzzyMatcherProvider[andOtherPatterns.length + 2];
        patterns[0] = this;
        patterns[1] = pattern;
        //noinspection ManualArrayCopy
        for (int i = 0; i < andOtherPatterns.length; i++) patterns[i + 2] = andOtherPatterns[i];
        boolean isIterative = true;
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i] == null)
                throw new IllegalArgumentException(OrdinalSuffix.EN.addTo(i + 1) + " pattern is null");
            isIterative = isIterative && patterns[i] instanceof IterativeFuzzyMatcherProvider;
        }
        if (isIterative) {
            return new IterativeMultiplePatterns(Arrays.copyOf(patterns, patterns.length, IterativeFuzzyMatcherProvider[].class));
        }
        // fall back to not iterative implementation
        return new MultiplePatterns(patterns);
    }
}
