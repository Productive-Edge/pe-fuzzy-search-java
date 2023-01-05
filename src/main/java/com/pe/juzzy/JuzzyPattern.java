package com.pe.juzzy;

public interface JuzzyPattern extends MatcherProvider {

    CharSequence text();

    int maxLevenshteinDistance();

    boolean caseInsensitive();

    static JuzzyPattern pattern(CharSequence pattern, int maxLevenshteinDistance) {
        return pattern(pattern, maxLevenshteinDistance, false);
    }

    static JuzzyPattern pattern(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        if (pattern == null)
            throw new IllegalArgumentException("pattern text can not be null");
        if (pattern.length() == 0)
            throw new IllegalArgumentException("pattern text can not be empty");
        if (pattern.length() <= 32)
            return new Bitap32(pattern, maxLevenshteinDistance, caseInsensitive);
        if (pattern.length() <= 64)
            return new Bitap64(pattern, maxLevenshteinDistance, caseInsensitive);
        return new UnlimitedBitap(pattern, maxLevenshteinDistance, caseInsensitive);
    }

    static MatcherProvider oneOf(JuzzyPattern first, JuzzyPattern orSecond, JuzzyPattern... orOthers) {
        IterativeJuzzyPattern[] patterns = new IterativeJuzzyPattern[orOthers.length + 2];
        patterns[0] = IterativeJuzzyPattern.cast(first, 1);
        patterns[1] = IterativeJuzzyPattern.cast(orSecond, 2);
        for (int i = 0; i < orOthers.length; i++) {
            final int index = i + 2;
            patterns[index] = IterativeJuzzyPattern.cast(orOthers[i], index);
        }
        return new MultiplePatterns(patterns);
    }

}