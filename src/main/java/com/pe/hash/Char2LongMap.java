package com.pe.hash;

import java.util.Arrays;

public final class Char2LongMap {

    private final FixedCharTable hash;
    private final long defaultValue;
    private final long[] values;

    public Char2LongMap(CharSequence charSequence, long defaultValue) {
        hash = FixedCharTable.from(charSequence);
        this.defaultValue = defaultValue;
        values = new long[hash.size()];
        Arrays.fill(values, defaultValue);
    }

    public void put(char key, long value) {
        values[hash.indexOf(key)] = value;
    }

    public long get(char key) {
        final int index = hash.indexOf(key);
        return index < 0 ? defaultValue : values[index];
    }
}
