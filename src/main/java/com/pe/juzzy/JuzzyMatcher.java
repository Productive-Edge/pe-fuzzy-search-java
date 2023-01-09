package com.pe.juzzy;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface JuzzyMatcher extends JuzzyResult {

    CharSequence text();

    default void reset() {
        reset(text(), 0, text().length());
    }
    default void reset(int fromIndex) {
        reset(text(), fromIndex, text().length());
    }
    default void reset(int fromIndex, int toIndex) {
        reset(text(), fromIndex, toIndex);
    }
    default void reset(CharSequence text) {
        reset(text, 0, text.length());
    }
    default void reset(CharSequence text, int fromIndex) {
        reset(text, fromIndex, text.length());
    }

    void reset(CharSequence text, int fromIndex, int toIndex);

    default Stream<JuzzyResult> stream() {
        return StreamSupport.stream(() -> new JuzzyResultSpliterator(this),
                JuzzyResultSpliterator.CHARACTERISTICS, false);
    }

    boolean find();

    default Optional<JuzzyResult> findTheBestMatching() {
        JuzzyResultRecord best = null;
        if(this instanceof IterativeJuzzyMatcher) {
            while (find()) {
                best = new JuzzyResultRecord(this);
                if(best.distance() == 0)
                    break;
                ((IterativeJuzzyMatcher)this).setMaxDistance(best.distance() - 1);
            }
        } else {
            while (find()) {
                if(best == null || best.distance() > distance()) {
                    best = new JuzzyResultRecord(this);
                    if(distance() == 0)
                        break;
                }
            }
        }
        return Optional.ofNullable(best);
    }
}
