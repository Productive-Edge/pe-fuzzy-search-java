package com.pe.text;


class IterativeMultiplePatterns implements FuzzyMultiPattern, IterativeFuzzyPattern {

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
            return this.text;
        }

        @Override
        public boolean find() {
            resetState();

            while (++this.index < this.maxIndex) {
                if (testNextSymbol()) {
                    return true;
                }
            }

            //insert at the end
            int maxDistance = 0;
            for (IterativeFuzzyMatcher matcher : this.matchers) {
                final int max = matcher.pattern().maxLevenshteinDistance();
                if (maxDistance < max) maxDistance = max;
            }

            for (int appendCount = 1; appendCount <= maxDistance; appendCount++) {
                if (testNextInsert(appendCount))
                    return true;
            }
            return false;
        }

        @Override
        public boolean started() {
            return this.index != -1;
        }

        @Override
        public boolean completed() {
            return this.matched == null;
        }

        @Override
        public void resetState() {
            this.matched = null;
            for (IterativeFuzzyMatcher matcher : this.matchers) matcher.resetState();
        }

        @Override
        public int getMaxDistance() {
            return ensureFound().getMaxDistance();
        }

        @Override
        public void setMaxDistance(int maxDistance) {
            for (IterativeFuzzyMatcher matcher : this.matchers) matcher.setMaxDistance(maxDistance);
        }

        @Override
        public boolean testNextSymbol() {
            for (IterativeFuzzyMatcher matcher : this.matchers) {
                matcher.setIndex(this.index);
                if (matcher.testNextSymbol()) {
                    this.matched = matcher;
                    int maxDistance = matcher.getMaxDistance();
                    matcher.improveResult(Math.min(this.index + matcher.pattern().text().length(), maxIndex));
                    matcher.setMaxDistance(maxDistance);
                    this.index = matcher.end() - 1;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean testNextInsert(final int iteration) {
            for (IterativeFuzzyMatcher matcher : this.matchers) {
                if (iteration <= matcher.pattern().maxLevenshteinDistance() && matcher.testNextInsert(iteration)) {
                    this.matched = matcher;
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
        public IterativeFuzzyMatcher ensureFound() {
            IterativeFuzzyMatcher.super.ensureFound();
            return this.matched;
        }

        @Override
        public FuzzyPattern pattern() {
            return ensureFound().pattern();
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
        public int distance() {
            return ensureFound().distance();
        }

        @Override
        public CharSequence foundText() {
            return ensureFound().foundText();
        }
    }

}
