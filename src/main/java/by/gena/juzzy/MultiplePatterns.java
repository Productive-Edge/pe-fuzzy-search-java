package by.gena.juzzy;

class MultiplePatterns implements MatcherProvider, IterativeJuzzyPattern {

    private final IterativeJuzzyPattern[] patterns;

    MultiplePatterns(IterativeJuzzyPattern[] patterns) {
        this.patterns = patterns;
    }

    @Override
    public JuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex) {
        return getIterativeMatcher(text, fromIndex, toIndex);
    }

    @Override
    public IterativeJuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex) {
        return new Matcher(text, fromIndex, toIndex);
    }

    class Matcher implements IterativeJuzzyMatcher {
        private CharSequence text;
        final IterativeJuzzyMatcher[] matchers;
        IterativeJuzzyMatcher matched;
        private int index;
        private int maxIndex;

        Matcher(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            index = Math.max(0, fromIndex) - 1;
            maxIndex = Math.min(text.length(), toIndex);
            matchers = new IterativeJuzzyMatcher[patterns.length];
            for (int i = 0, l = matchers.length; i < l; i++) matchers[i] = patterns[i].getIterativeMatcher(text, index, maxIndex);
        }

        @Override
        public CharSequence text() {
            return text;
        }

        @Override
        public void reset(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            index = Math.max(0, fromIndex) - 1;
            maxIndex = Math.min(text.length(), maxIndex);
            for (IterativeJuzzyMatcher matcher : matchers) matcher.reset(text, index, maxIndex);
        }

        @Override
        public boolean find() {
            resetState();

            while (++index < maxIndex) {
                if (testNextSymbol()) {
                    return true;
                }
            }

            //insert at the end
            int maxDistance = 0;
            for (IterativeJuzzyMatcher matcher : matchers) {
                final int max = matcher.pattern().maxLevenshteinDistance();
                if(maxDistance < max) maxDistance = max;
            }

            for (int appendCount = 1; appendCount <= maxDistance; appendCount++) {
                if (testNextInsert(appendCount))
                    return true;
            }
            return false;
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

        @Override
        public JuzzyPattern pattern() {
            return ensureFound().pattern();
        }

        @Override
        public void resetState() {
            matched = null;
            for (IterativeJuzzyMatcher matcher : matchers) matcher.resetState();
        }

        @Override
        public int getMaxDistance() {
            return ensureFound().getMaxDistance();
        }

        @Override
        public void setMaxDistance(int maxDistance) {
            for (IterativeJuzzyMatcher matcher : matchers) matcher.setMaxDistance(maxDistance);
        }

        @Override
        public boolean testNextSymbol() {
            for (IterativeJuzzyMatcher matcher : matchers) {
                matcher.reset(index);
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
        public boolean testNextInsert(final int iteration) {
            for (IterativeJuzzyMatcher matcher : matchers) {
                if (iteration <= matcher.pattern().maxLevenshteinDistance() && matcher.testNextInsert(iteration)) {
                    matched = matcher;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void improveResult(int maxIndex) {
            ensureFound().improveResult(maxIndex);
        }

        private IterativeJuzzyMatcher ensureFound() {
            if(index == -1)
                throw new IllegalStateException("find method must be called before");
            if(matched == null)
                throw new IllegalStateException("no matches were found, last call of find method returned false");
            return matched;
        }
    }

}
