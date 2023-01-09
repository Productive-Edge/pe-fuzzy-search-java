package com.pe.text;

public interface MatcherProvider {
    default FuzzyMatcher matcher(CharSequence text) {
        return matcher(text, 0, text.length());
    }
    default FuzzyMatcher matcher(CharSequence text, int fromIndex) {
        return matcher(text, fromIndex, text.length());
    }
    FuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex);
}
