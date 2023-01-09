package com.pe.juzzy;

public interface MatcherProvider {
    default JuzzyMatcher matcher(CharSequence text) {
        return matcher(text, 0, text.length());
    }
    default JuzzyMatcher matcher(CharSequence text, int fromIndex) {
        return matcher(text, fromIndex, text.length());
    }
    JuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex);
}
