package com.pe.text;

interface DefaultFuzzyMatcher extends FuzzyMatcher {
    default FuzzyMatcher ensureFound() {
        if (!started())
            throw new IllegalStateException("FuzzyMatcher.stream() or FuzzyMatcher.find() methods must be called before retrieving matching result");
        if (completed())
            throw new IllegalStateException("No matching result in the completed matcher instance");
        return this;
    }
}
