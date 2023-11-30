package com.pe.text;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Base abstract implementation of the Fuzzy Pattern and Matcher
 */
abstract class BaseBitap implements FuzzyPattern, IterativeFuzzyMatcherProvider {
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
    public String toString() {
        return getClass().getName() + "{pattern=\"" + pattern +
                "\", maxLevenshteinDistance=" + maxLevenshteinDistance +
                ", caseInsensitive=" + caseInsensitive +
                '}';
    }

    abstract class Matcher implements IterativeFuzzyMatcher {

        /**
         * temporal copy of values in the {@link #lengthChanges}
         */
        private final int[] lengthChangesCopy;
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
         * contains changes in length (or applied operations DELETION, REPLACEMENT, INSERT) for the matched text:
         * <ul>
         *     <li><b>-1</b> symbol was deleted</li>
         *     <li><b> 0</b> symbol was replaced or matched</li>
         *     <li><b> 1</b> symbol was inserted</li>
         * </ul>
         * values starts from 1st index to match with count of operations (Levenshtein distance)
         */
        int[] lengthChanges;
        /**
         * start search index (search begins from this position in the {@link #text})
         */
        private int fromIndex;

        protected Matcher(CharSequence text, int fromIndex, int toIndex) {
            maxDistance = maxLevenshteinDistance;
            final int n = maxDistance + 1;
            lengthChanges = new int[n];
            lengthChangesCopy = new int[n];
            reset(text, fromIndex, toIndex);
        }

        @Override
        public void reset(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            this.fromIndex = Math.max(0, fromIndex);
            this.index = this.fromIndex - 1;
            this.toIndex = Math.max(fromIndex, Math.min(text.length(), toIndex));
        }

        @Override
        public CharSequence text() {
            return text;
        }

        @Override
        public boolean find() {
            resetState();
            if (toIndex - index <= 1) return false;
            while (++index < toIndex) {
                if (testNextSymbol()) {
                    final int maxDistanceCopy = maxDistance;
                    improveResult(Math.min(index + BaseBitap.this.pattern.length() + maxLevenshteinDistance + 1 - levenshteinDistance, toIndex));
                    maxDistance = maxDistanceCopy;
                    return true;
                }
            }
            return false;
        }

        @Override
        public int to() {
            return toIndex;
        }

        @Override
        public boolean started() {
            return index >= fromIndex;
        }

        @Override
        public boolean completed() {
            return index >= toIndex;
        }

        @Override
        public int from() {
            return fromIndex;
        }

        @Override
        public void resetState() {
            //fill with insertions
            setInsertsAfter(0);
        }

        @Override
        public void improveResult(int maxIndex) {
            if (levenshteinDistance == 0)
                return;
            if (index + 1 == maxIndex)
                return;
            // store
            final int indexCopy = index;
            final int levenshteinDistanceCopy = levenshteinDistance;
            final int totalLengthChangesCopy = sumLengthChanges();
            // loop is faster on small arrays
            //noinspection ManualArrayCopy
            for (int i = 1; i <= levenshteinDistanceCopy; i++) lengthChangesCopy[i] = lengthChanges[i];

            maxDistance = levenshteinDistance;
            while (++index < maxIndex) {
                if (testNextSymbol()) {
                    if (levenshteinDistance < levenshteinDistanceCopy || sumLengthChanges() < totalLengthChangesCopy) {
                        improveResult(maxIndex);
                        return;
                    }
                } else {
                    break;
                }
            }
            // restore
            index = indexCopy;
            levenshteinDistance = levenshteinDistanceCopy;
            // loop is faster on small arrays
            //noinspection ManualArrayCopy
            for (int i = 1; i <= levenshteinDistanceCopy; i++) lengthChanges[i] = lengthChangesCopy[i];

        }

        @Override
        public void setIndex(int index) {
            this.index = index;
        }

        @Override
        public int getMaxDistance() {
            return maxDistance;
        }

        @Override
        public void setMaxDistance(int maxDistance) {
            this.maxDistance = maxDistance;
        }

        /**
         * Sets insertion operations after the specified operation by index
         *
         * @param index of the operation which should be unchanged,
         *              all following operations have to be reset to the insertions
         */
        protected final void setInsertsAfter(int index) {
            for (int i = index + 1; i <= maxDistance; i++) lengthChanges[i] = 1;
        }

        @Override
        public String toString() {
            return getClass().getName() + '{' +
                    "edits=" + Arrays.toString(streamEditTypes().toArray()) +
                    ", text=\"" + text +
                    "\", distance={current=" + levenshteinDistance + ", max=" + maxDistance +
                    "}, index=" + index +
                    " in [" + fromIndex + '-' + toIndex + "]}";
        }

        @Override
        public FuzzyPattern pattern() {
            return BaseBitap.this;
        }


        @Override
        public int start() {
            return end() - BaseBitap.this.pattern.length() + sumLengthChanges();
        }

        @Override
        public int end() {
            ensureFound();
            return index + 1;
        }

        @Override
        public int distance() {
            return levenshteinDistance;
        }

        @Override
        public CharSequence foundText() {
            return text.subSequence(start(), end());
        }

        /**
         * Sum of the all length changes, which is the difference in length between pattern and this matching
         *
         * @return Sum of the all length changes
         */
        protected int sumLengthChanges() {
            int result = 0;
            for (int i = 1; i <= levenshteinDistance; i++) result += lengthChanges[i];
            return result;
        }

        @Override
        public Stream<OperationType> streamEditTypes() {
            if (levenshteinDistance == 0)
                return Stream.empty();
            return Arrays.stream(lengthChanges, 1, levenshteinDistance + 1)
                    .mapToObj(change -> OperationType.values[change + 2]);
        }
    }

}
