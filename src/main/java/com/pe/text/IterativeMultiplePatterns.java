package com.pe.text;


import java.util.stream.Stream;

/**
 * Internal implementation of the {@link IterativeFuzzyPattern} and {@link IterativeFuzzyMatcher}
 */
class IterativeMultiplePatterns implements FuzzyPatterns, IterativeFuzzyPattern {

    private final IterativeFuzzyPattern[] patterns;

    IterativeMultiplePatterns(IterativeFuzzyPattern[] patterns) {
        this.patterns = patterns;
    }

    @Override
    public IterativeFuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex) {
        return new Matcher(text, fromIndex, toIndex);
    }

    class Matcher implements IterativeFuzzyMatcher {
        final IterativeFuzzyMatcher[] matchers;
        IterativeFuzzyMatcher matched;
        private CharSequence text;
        private int index;
        private int maxIndex;

        Matcher(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            this.index = Math.max(0, fromIndex) - 1;
            this.maxIndex = Math.min(text.length(), toIndex);
            this.matchers = new IterativeFuzzyMatcher[patterns.length];
            for (int i = 0, l = matchers.length; i < l; i++)
                matchers[i] = patterns[i].getIterativeMatcher(text, fromIndex, maxIndex);
        }

        @Override
        public void reset(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            this.index = Math.max(0, fromIndex) - 1;
            this.maxIndex = Math.min(text.length(), toIndex);
            for (IterativeFuzzyMatcher matcher : matchers) matcher.reset(text, fromIndex, toIndex);
        }

        @Override
        public CharSequence text() {
            return text;
        }

        @Override
        public boolean find() {
            resetState();
            while (++index < maxIndex) {
                if (testNextSymbol()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int to() {
            return maxIndex;
        }

        @Override
        public boolean started() {
            return index != -1;
        }

        @Override
        public boolean completed() {
            return matched == null;
        }

        @Override
        public int from() {
            return index;
        }

        @Override
        public void resetState() {
            matched = null;
            for (IterativeFuzzyMatcher matcher : matchers) matcher.resetState();
        }

        @Override
        public boolean testNextSymbol() {
            for (IterativeFuzzyMatcher matcher : matchers) {
                matcher.setIndex(index);
                if (matcher.testNextSymbol()) {
                    matched = matcher;
                    int maxDistance = matcher.getMaxDistance();
                    matcher.improveResult(Math.min(index + matcher.pattern().text().length(), maxIndex));
                    matcher.setMaxDistance(maxDistance);
                    index = matcher.end() - 1;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void improveResult(int maxIndex) {
            ensureFound().improveResult(maxIndex);
        }

        @Override
        public void setIndex(int index) {
            this.index = index;
        }

        @Override
        public int getMaxDistance() {
            return ensureFound().getMaxDistance();
        }

        @Override
        public void setMaxDistance(int maxDistance) {
            for (IterativeFuzzyMatcher matcher : matchers) matcher.setMaxDistance(maxDistance);
        }

        @Override
        public IterativeFuzzyMatcher ensureFound() {
            IterativeFuzzyMatcher.super.ensureFound();
            return matched;
        }

        @Override
        public int start() {
            return ensureFound().start();
        }

        @Override
        public int end() {
            return ensureFound().end();
        }

        @Override
        public CharSequence foundText() {
            return ensureFound().foundText();
        }

        @Override
        public FuzzyPattern pattern() {
            return ensureFound().pattern();
        }

        @Override
        public int distance() {
            return ensureFound().distance();
        }

        @Override
        public Stream<OperationType> streamEditTypes() {
            return ensureFound().streamEditTypes();
        }
    }

}
