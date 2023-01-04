package by.gena.juzzy;

import by.gena.ordinal.Ordinal;

interface IterativeJuzzyPattern {
    IterativeJuzzyMatcher getIterativeMatcher(CharSequence text);

    static IterativeJuzzyPattern cast(JuzzyPattern pattern, int index) {
        if (pattern == null)
            throw new IllegalArgumentException(Ordinal.en(index) + " pattern is null");
        if (pattern instanceof IterativeJuzzyPattern)
            return (IterativeJuzzyPattern) pattern;
        throw new IllegalArgumentException(Ordinal.en(index) + " pattern " + pattern.getClass().getName()
                + " is not supported, since it doesn't implement "
                + IterativeJuzzyPattern.class.getName() + " interface");
    }
}