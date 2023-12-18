package com.pe.text;

/**
 * Immutable records contains information about character and its index
 */
public final class CharWithIndex {

    private final char value;
    private final int index;

    public CharWithIndex(char value, int index) {
        this.value = value;
        this.index = index;
    }

    public char value() {
        return value;
    }

    public int index() {
        return index;
    }

    @Override
    public int hashCode() {
        int result = value;
        result = 31 * result + index;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CharWithIndex charWithIndex = (CharWithIndex) o;

        if (value != charWithIndex.value) return false;
        return index == charWithIndex.index;
    }

    @Override
    public String toString() {
        return "{'" + value + "' at " + index + '}';
    }
}
