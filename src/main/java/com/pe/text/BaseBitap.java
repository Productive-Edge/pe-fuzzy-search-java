package com.pe.text;

/**
 * base abstract implementation of Bitap pattern and matcher
 */
abstract class BaseBitap implements FuzzyPattern, IterativeFuzzyPattern {
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

    abstract class Matcher implements IterativeFuzzyMatcher {

        /**
         * temporal copy of values in the {@link #lengthChanges}
         */
        private final int[] lengthChangesCopy;
        /**
         * contains changes in length (or applied operations DELETION, REPLACEMENT, INSERT) for the matched text:
         * <ul>
         *     <li><b>-1</b> symbol was deleted</li>
         *     <li><b> 0</b> symbol was replaced or matched</li>
         *     <li><b> 1</b> symbol was inserted</li>
         * </ul>
         * values starts from 1st index to match with count of operations (Levenshtein distance)
         */
        protected int[] lengthChanges;

        protected CharSequence text;
        /**
         * current Levenshtein distance
         */
        protected int levenshteinDistance;
        /**
         * maximum allowed Levenshtein distance
         */
        protected int maxDistance;

        /**
         * current search index (i.e. end) in the {@link #text}
         */
        protected int index;

        /**
         * stop search index (search is stopped by reaching this position in the {@link #text})
         */
        protected int toIndex;

        /**
         * start search index (search begins from this position in the {@link #text})
         */
        private int fromIndex;

        protected Matcher(CharSequence text, int fromIndex, int toIndex) {
            this.maxDistance = maxLevenshteinDistance;
            final int n = this.maxDistance + 1;
            this.lengthChanges = new int[n];
            this.lengthChangesCopy = new int[n];
            reset(text, fromIndex, toIndex);
        }

        @Override
        public void reset(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            this.fromIndex = Math.max(0, fromIndex);
            this.index = this.fromIndex - 1;
            this.toIndex = Math.max(this.fromIndex, Math.min(text.length(), toIndex));
        }

        @Override
        public CharSequence text() {
            return this.text;
        }

        @Override
        public boolean find() {
            resetState();
            if (this.toIndex - this.index <= 1) return false;
            while (++this.index < this.toIndex) {
                if (testNextSymbol()) {
                    final int maxDistanceCopy = this.maxDistance;
                    improveResult(Math.min(this.index + BaseBitap.this.pattern.length() + maxLevenshteinDistance + 1 - this.levenshteinDistance, this.toIndex));
                    maxDistance = maxDistanceCopy;
                    return true;
                }
            }

            return false;
        }

        @Override
        public int to() {
            return this.toIndex;
        }

        @Override
        public boolean started() {
            return this.index >= this.fromIndex;
        }

        @Override
        public boolean completed() {
            return this.index >= this.toIndex;
        }

        @Override
        public int from() {
            return this.fromIndex;
        }

        @Override
        public void resetState() {
            //fill with insertions
            setInsertsAfter(0);
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
            final int totalLengthChangesCopy = this.totalLengthChanges();
            // loop is faster on small arrays
            for (int i = 1; i <= levenshteinDistanceCopy; i++) this.lengthChangesCopy[i] = this.lengthChanges[i];

            this.maxDistance = this.levenshteinDistance;
            while (++this.index < maxIndex) {
                if (testNextSymbol()) {
                    if (this.levenshteinDistance < levenshteinDistanceCopy || this.totalLengthChanges() < totalLengthChangesCopy) {
                        improveResult(maxIndex);
                        return;
                    }
                } else {
                    break;
                }
            }
            // restore
            this.index = indexCopy;
            this.levenshteinDistance = levenshteinDistanceCopy;
            for (int i = 1; i <= levenshteinDistanceCopy; i++) this.lengthChanges[i] = this.lengthChangesCopy[i];

        }

        @Override
        public void setIndex(int index) {
            this.index = index;
        }

        @Override
        public int getMaxDistance() {
            return this.maxDistance;
        }

        @Override
        public void setMaxDistance(int maxDistance) {
            this.maxDistance = maxDistance;
        }

        protected final void setInsertsAfter(int index) {
            for (int i = index + 1; i <= this.maxDistance; i++) this.lengthChanges[i] = 1;
        }

        @Override
        public FuzzyPattern pattern() {
            return BaseBitap.this;
        }


        @Override
        public int start() {
            return end() - BaseBitap.this.pattern.length() + totalLengthChanges();
        }

        @Override
        public int end() {
            ensureFound();
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

        protected int totalLengthChanges() {
            int result = 0;
            for (int i = 1; i <= this.levenshteinDistance; i++) result += this.lengthChanges[i];
            return result;
        }

    }
}
