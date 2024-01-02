package com.pe.hash;

public final class FCT1 implements FixedCharTable {
    private final int single;

    FCT1(int single) {
        this.single = single;
    }

    @Override
    public int indexOf(char c) {
        return single == c ? 0 : -1;
    }

    @Override
    public int size() {
        return 1;
    }
}
