package com.pe.text;

import java.util.Spliterators;
import java.util.function.Consumer;

class OperationsSpliterator extends Spliterators.AbstractSpliterator<Operation> {

    static final int CHARACTERISTICS = ORDERED | NONNULL | IMMUTABLE | SIZED;
    private final OperationsIterator iterator;

    OperationsSpliterator(OperationsIterator iterator) {
        super(iterator.size(), CHARACTERISTICS);
        this.iterator = iterator;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Operation> action) {
        if (!iterator.hasNext()) return false;
        action.accept(iterator.next());
        return true;
    }
}
