package com.pe.text;

import com.pe.ordinal.Ordinal;

interface IterativeFuzzyPattern {
    IterativeFuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex);

    static IterativeFuzzyPattern cast(final FuzzyPattern pattern, final int index) {
        if (pattern == null)
            throw new IllegalArgumentException(Ordinal.en(index) + " pattern is null");
        if (pattern instanceof IterativeFuzzyPattern)
            return (IterativeFuzzyPattern) pattern;
        throw new IllegalArgumentException(Ordinal.en(index) + " pattern " + pattern.getClass().getName()
                + " is not supported, since it doesn't implement "
                + IterativeFuzzyPattern.class.getName() + " interface");
    }
}