package com.pe.text;

import java.util.Arrays;
import java.util.stream.Stream;

class FuzzyResultRecord implements FuzzyResult {

    private final FuzzyPattern pattern;
    private final int start;
    private final int end;
    private final int distance;
    private final CharSequence foundText;
    private final int[] edits;

    FuzzyResultRecord(FuzzyMatcher matcher) {
        this.pattern = matcher.pattern();
        this.start = matcher.start();
        this.end = matcher.end();
        this.distance = matcher.distance();
        this.foundText = matcher.foundText();
        this.edits = matcher.streamEdits().mapToInt(OperationType::ordinal).toArray();
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
    public CharSequence foundText() {
        return foundText;
    }

    @Override
    public FuzzyPattern pattern() {
        return pattern;
    }

    @Override
    public int distance() {
        return distance;
    }

    @Override
    public Stream<OperationType> streamEdits() {
        return Arrays.stream(edits).mapToObj(ordinal -> OperationType.values[ordinal]);
    }

    @Override
    public String toString() {
        return "FuzzyResultRecord{" + pattern.toString() +
                ", start=\"" + start + '"' +
                ", end=" + end +
                ", distance=" + distance +
                ", foundText=\"" + foundText + '"' +
                ", edits=" + Arrays.toString(streamEdits().toArray()) + "}";
    }
}
