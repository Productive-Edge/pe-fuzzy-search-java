package com.pe.juzzy;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface MatcherProvider {
    default JuzzyMatcher matcher(CharSequence text) {
        return matcher(text, 0, text.length());
    }
    default JuzzyMatcher matcher(CharSequence text, int fromIndex) {
        return matcher(text, fromIndex, text.length());
    }
    JuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex);

    default Stream<JuzzyResult> streamMatches(CharSequence text) {
        return StreamSupport.stream(() -> new JuzzyResultSpliterator(matcher(text)),
                JuzzyResultSpliterator.CHARACTERISTICS, false);
    }
}
