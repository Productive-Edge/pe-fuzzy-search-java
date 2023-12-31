package com.pe.hash;

public final class FCT2 implements FixedCharTable {

    private final int first;
    private final int second;

    FCT2(final int first, final int second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int indexOf(char c) {
        if (c == first) return 0;
        return c == second ? 1 : -1;
    }

    @Override
    public int size() {
        return 2;
    }
}
