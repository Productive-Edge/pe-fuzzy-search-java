package com.pe.hash;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;

public final class Char2ObjMap<T> {

    private final FixedCharTable hash;
    private final T[] values;
    private final T defaultValue;

    public Char2ObjMap(CharSequence charSequence, Class<T> clazz, T defaultValue) {
        hash = FixedCharTable.from(charSequence);
        values = (T[]) Array.newInstance(clazz, hash.size());
        this.defaultValue = defaultValue;
        Arrays.fill(values, defaultValue);
    }

    public void put(char key, T value) {
        values[hash.indexOf(key)] = value;
    }

    public T get(char key) {
        final int index = hash.indexOf(key);
        return index < 0 ? defaultValue : values[index];
    }

    public T computeIfAbsent(char key, CharFunction<T> producer) {
        final int index = hash.indexOf(key);
        if (Objects.equals(values[index], defaultValue))
            values[index] = producer.apply(key);
        return values[index];
    }
}
