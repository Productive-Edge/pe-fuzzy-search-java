package by.gena.juzzy;

final class MultiplePatterns implements MatcherProvider, IterativeJuzzyPattern {

    private final IterativeJuzzyPattern[] patterns;

    MultiplePatterns(IterativeJuzzyPattern[] patterns) {
        this.patterns = patterns;
    }

    @Override
    public JuzzyMatcher matcher(CharSequence text) {
        return getIterativeMatcher(text);
    }

    @Override
    public IterativeJuzzyMatcher getIterativeMatcher(CharSequence text) {
        return new Matcher(text);
    }

    class Matcher implements IterativeJuzzyMatcher {

        final IterativeJuzzyMatcher[] matchers;
        IterativeJuzzyMatcher matched;
        private int end;
        private final int textLength;

        Matcher(CharSequence text) {
            textLength = text.length();
            matchers = new IterativeJuzzyMatcher[patterns.length];
            for (int i = 0, l = matchers.length; i < l; i++) matchers[i] = patterns[i].getIterativeMatcher(text);
            setFrom(-1);
        }

        @Override
        public JuzzyPattern pattern() {
            return ensureFound().pattern();
        }

        @Override
        public boolean find() {
            resetState();

            while (++end < textLength) {
                if(testNextSymbol())
                    return true;
            }

            //insert at the end
            int maxLevenshteinDistance = 0;
            for (IterativeJuzzyMatcher matcher : matchers) {
                matcher.setFrom(end);
                final int max = matcher.pattern().maxLevenshteinDistance();
                if(maxLevenshteinDistance < max) maxLevenshteinDistance = max;
            }
            for (int appendCount = 1; appendCount <= maxLevenshteinDistance; appendCount++) {
                if (testNextInsert(appendCount))
                    return true;
            }
            return false;
        }

        @Override
        public boolean find(int fromIndex) {
            setFrom(Math.max(0, fromIndex) - 1);
            return find();
        }

        @Override
        public boolean find(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException();
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
        public void setFrom(int atIndex) {
            end = atIndex;
        }

        @Override
        public void setTo(int lastIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void resetState() {
            matched = null;
            for (IterativeJuzzyMatcher matcher : matchers) matcher.resetState();
        }

        @Override
        public boolean testNextSymbol() {
            for (IterativeJuzzyMatcher matcher : matchers) {
                matcher.setFrom(end);
                if (matcher.testNextSymbol()) {
                    matched = matcher;
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

        private IterativeJuzzyMatcher ensureFound() {
            if(end == -1)
                throw new IllegalStateException("find method must be called before");
            if(matched == null)
                throw new IllegalStateException("no matches were found, last call of find method returned false");
            return matched;
        }
    }

}
