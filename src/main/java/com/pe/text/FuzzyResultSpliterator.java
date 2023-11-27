package com.pe.text;

import java.util.Spliterators;
import java.util.function.Consumer;

class FuzzyResultSpliterator extends Spliterators.AbstractSpliterator<FuzzyResult> {

    static final int CHARACTERISTICS = ORDERED | NONNULL | IMMUTABLE;
    private final FuzzyMatcher matcher;

    FuzzyResultSpliterator(FuzzyMatcher matcher) {
        super(Long.MAX_VALUE, CHARACTERISTICS);
        this.matcher = matcher;
    }

    @Override
    public boolean tryAdvance(Consumer<? super FuzzyResult> action) {
        if (!matcher.find()) return false;
        action.accept(new FuzzyResultRecord(matcher));
        return true;
    }
}
