package by.gena.juzzy;

import java.util.Spliterators;
import java.util.function.Consumer;

class JuzzyResultSpliterator extends Spliterators.AbstractSpliterator<JuzzyResult> {

    static final int CHARACTERISTICS = ORDERED | DISTINCT | NONNULL | IMMUTABLE;
    private JuzzyMatcher matcher;

    JuzzyResultSpliterator(JuzzyMatcher matcher) {
        super(Long.MAX_VALUE,  CHARACTERISTICS);
        this.matcher = matcher;
    }

    @Override
    public boolean tryAdvance(Consumer<? super JuzzyResult> action) {
        if(!matcher.find()) return false;
        action.accept(new JuzzyResultRecord(matcher));
        return true;
    }
}
