package com.pe.hash;

import java.util.Arrays;

public final class Char2IntMap {

    private final FixedCharTable hash;
    private final int defaultValue;
    private final int[] values;

    public Char2IntMap(CharSequence charSequence, int defaultValue) {
        hash = FixedCharTable.from(charSequence);
        this.defaultValue = defaultValue;
        values = new int[hash.size()];
        Arrays.fill(values, defaultValue);
    }

    public void put(char key, int value) {
        values[hash.indexOf(key)] = value;
    }

    public int get(char key) {
        final int index = hash.indexOf(key);
        return index < 0 ? defaultValue : values[index];
    }
}
