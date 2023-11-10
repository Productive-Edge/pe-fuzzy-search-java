package com.pe.text;

public abstract class BaseBitap implements FuzzyPattern, IterativeFuzzyPattern {
    private final CharSequence pattern;
    private final int maxLevenshteinDistance;
    private final boolean caseInsensitive;

    protected BaseBitap(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        this.pattern = pattern;
        this.maxLevenshteinDistance = maxLevenshteinDistance;
        this.caseInsensitive = caseInsensitive;
    }

    @Override
    public CharSequence text() {
        return pattern;
    }

    @Override
    public int maxLevenshteinDistance() {
        return maxLevenshteinDistance;
    }

    @Override
    public boolean caseInsensitive() {
        return caseInsensitive;
    }

    @Override
    public FuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex) {
        return getIterativeMatcher(text, fromIndex, toIndex);
    }

    abstract class Matcher implements IterativeFuzzyMatcher {

        protected CharSequence text;
        protected final int[] lengthChanges;
        private final int[] lengthChangesCopy;
        protected int levenshteinDistance;
        protected int maxDistance;
        protected int index;
        protected int maxIndex;

        protected Matcher(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            this.maxDistance = maxLevenshteinDistance;
            final int n = this.maxDistance + 1;
            this.lengthChanges = new int[n];
            this.lengthChangesCopy = new int[n];
            this.index = Math.max(0, fromIndex) - 1;
            this.maxIndex = Math.min(text.length(), toIndex);
        }

        @Override
        public void reset(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            this.index = Math.max(0, fromIndex) - 1;
            this.maxIndex = Math.min(text.length(), toIndex);
        }

        @Override
        public void resetState() {
            for (int i = 1; i <= this.maxDistance; i++) this.lengthChanges[i] = 0;
        }

        public int getMaxDistance() {
            return this.maxDistance;
        }

        public void setMaxDistance(int maxDistance) {
            this.maxDistance = maxDistance;
        }

        @Override
        public boolean find() {
            resetState();
            while (++this.index < this.maxIndex) {
                if (testNextSymbol()) {
                    final int maxDistanceCopy = this.maxDistance;
                    improveResult(Math.min(this.index + BaseBitap.this.pattern.length(), this.maxIndex));
                    maxDistance = maxDistanceCopy;
                    return true;
                }
            }

            //insert at the end
            for (int appendCount = 1; appendCount <= this.maxDistance; appendCount++) {
                if (testNextInsert(appendCount)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void improveResult(int maxIndex) {
            if (this.levenshteinDistance == 0)
                return;
            if (this.index + 1 == maxIndex)
                return;
            // store
            final int indexCopy = this.index;
            final int levenshteinDistanceCopy = this.levenshteinDistance;
            // loop is faster on small arrays
            for (int i = 1; i < levenshteinDistanceCopy; i++) this.lengthChangesCopy[i] = this.lengthChanges[i];

            this.maxDistance = this.levenshteinDistance - 1;
            while (++this.index < maxIndex) {
                if (testNextSymbol()) {
                    improveResult(maxIndex);
                    return;
                }
            }

            // restore
            this.index = indexCopy;
            this.levenshteinDistance = levenshteinDistanceCopy;
            // loop is faster on small arrays
            for (int i = 1; i < levenshteinDistanceCopy; i++) this.lengthChanges[i] = this.lengthChangesCopy[i];
        }

        @Override
        public void setIndex(int index) {
            this.index = index;
        }

        @Override
        public int start() {
            return end() - BaseBitap.this.pattern.length() + totalLengthChanges();
        }

        private int totalLengthChanges() {
            int lengthChange = this.levenshteinDistance == 0 ? 0 : this.lengthChanges[1];
            for (int i = 2; i <= this.levenshteinDistance; i++) lengthChange += this.lengthChanges[i];
            return lengthChange;
        }

        @Override
        public int end() {
            if (this.index == -1)
                throw new IllegalStateException("find method must be called before index");
            if (this.index >= this.maxIndex)
                throw new IllegalStateException("no matches were found, last call of the find method returned false");
            return this.index + 1;
        }

        @Override
        public int distance() {
            return this.levenshteinDistance;
        }

        @Override
        public CharSequence foundText() {
            return this.text.subSequence(start(), end());
        }

        @Override
        public FuzzyPattern pattern() {
            return BaseBitap.this;
        }

        @Override
        public CharSequence text() {
            return this.text;
        }

    }
}
