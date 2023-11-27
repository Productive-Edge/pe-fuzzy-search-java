package com.pe.text;

/**
 * Default implementation of {@code ensureFound} method which is used in the {@link IterativeFuzzyMatcher}
 * and {@link MultiplePatterns.Matcher}
 */
interface DefaultFuzzyMatcher extends FuzzyMatcher {

    /**
     * Ensures that matcher is in state with successfully found matching, i.e. has started and not completed yet.
     *
     * @return this matcher instance
     */
    default FuzzyMatcher ensureFound() {
        if (!started())
            throw new IllegalStateException("FuzzyMatcher.stream() or FuzzyMatcher.find() methods must be called before retrieving matching result");
        if (completed())
            throw new IllegalStateException("No matching result in the completed matcher instance");
        return this;
    }
}
