package com.pe.text;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface FuzzyMatcher extends FuzzyResult {

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

    default Stream<FuzzyResult> stream() {
        return StreamSupport.stream(() -> new FuzzyResultSpliterator(this),
                FuzzyResultSpliterator.CHARACTERISTICS, false);
    }

    boolean find();

    default Optional<FuzzyResult> findTheBestMatching() {
        FuzzyResultRecord best = null;
        if(this instanceof IterativeFuzzyMatcher) {
            while (find()) {
                best = new FuzzyResultRecord(this);
                if(best.distance() == 0)
                    break;
                ((IterativeFuzzyMatcher)this).setMaxDistance(best.distance() - 1);
            }
        } else {
            while (find()) {
                if(best == null || best.distance() > distance()) {
                    best = new FuzzyResultRecord(this);
                    if(distance() == 0)
                        break;
                }
            }
        }
        return Optional.ofNullable(best);
    }
}
