package com.pe.juzzy;

class JuzzyResultRecord implements JuzzyResult {

    private final JuzzyPattern pattern;
    private final int start;
    private final int end;
    private final int distance;
    private final CharSequence foundText;

    JuzzyResultRecord(JuzzyMatcher matcher) {
        this.pattern = matcher.pattern();
        this.start = matcher.start();
        this.end = matcher.end();
        this.distance = matcher.distance();
        this.foundText = matcher.foundText();

    }

    @Override
    public JuzzyPattern pattern() {
        return pattern;
    }

    @Override
    public int start() {
        return start;
    }

    @Override
    public int end() {
        return end;
    }

    @Override
    public int distance() {
        return distance;
    }

    @Override
    public CharSequence foundText() {
        return foundText;
    }
}
