package com.pe.text;

@Deprecated
public class Levenshtein {

    @Deprecated
    public static int distance(CharSequence s1, CharSequence s2) {
        if (s1 == null || s2 == null)
            throw new IllegalArgumentException("CharSequences must not be null");
        int l1 = s1.length();
        int l2 = s2.length();
        if (l1 == 0) return l2;
        if (l2 == 0) return l1;
        if (l2 < l1) {
            final CharSequence tmpCS = s1;
            s1 = s2;
            s2 = tmpCS;
            l1 = l2;
            l2 = s2.length();
        }
        FuzzyPattern pattern = FuzzyPattern.compile(s1, l1);
        @SuppressWarnings("OptionalGetWithoutIsPresent") FuzzyResult result = pattern.matcher(s2).findTheBest().get();
        return l2 - result.end() + result.start() + result.distance();
    }

}
