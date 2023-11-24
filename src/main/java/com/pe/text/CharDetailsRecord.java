package com.pe.text;

class CharDetailsRecord implements CharDetails {
    private final char value;
    private final int index;

    CharDetailsRecord(char value, int index) {
        this.value = value;
        this.index = index;
    }

    @Override
    public char value() {
        return value;
    }

    @Override
    public int index() {
        return index;
    }
}
