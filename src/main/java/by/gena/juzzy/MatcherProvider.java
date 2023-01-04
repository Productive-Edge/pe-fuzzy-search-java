package by.gena.juzzy;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface MatcherProvider {
    JuzzyMatcher matcher(CharSequence text);
    default Stream<JuzzyResult> streamMatches(final CharSequence text) {
        return StreamSupport.stream(() -> new JuzzyResultSpliterator(matcher(text)),
                JuzzyResultSpliterator.CHARACTERISTICS, false);
    }
}
