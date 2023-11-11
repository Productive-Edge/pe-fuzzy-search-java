package com.pe.text;

class FuzzyResultRecord implements FuzzyResult {

    private final FuzzyPattern pattern;
    private final int start;
    private final int end;
    private final int distance;
    private final CharSequence foundText;

    FuzzyResultRecord(FuzzyMatcher matcher) {
        this.pattern = matcher.pattern();
        this.start = matcher.start();
        this.end = matcher.end();
        this.distance = matcher.distance();
        this.foundText = matcher.foundText();
    }

    @Override
    public FuzzyPattern pattern() {
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

    @Override
    public String toString() {
        return "FuzzyResultRecord{" +
                "pattern=" + pattern.text() +
                ", start=" + start +
                ", end=" + end +
                ", distance=" + distance +
                ", foundText=" + foundText +
                '}';
    }
}
