package com.pe.hash;

final class FCT0 implements FixedCharTable {
    static final FCT0 INSTANCE = new FCT0();

    @Override
    public int indexOf(char c) {
        return -1;
    }

    @Override
    public int size() {
        return 0;
    }
}
