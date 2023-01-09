package com.pe.text;

import it.unimi.dsi.fastutil.chars.Char2LongOpenHashMap;

class Bitap64 implements FuzzyPattern, IterativeFuzzyPattern {

    private final Char2LongOpenHashMap positionBitMasks;
    private final CharSequence pattern;
    private final int patternLength;
    private final long lastBitMask;
    private final int maxLevenshteinDistance;
    private final boolean caseInsensitive;

    Bitap64(CharSequence pattern, int maxLevenshteinDistance) {
        this(pattern, maxLevenshteinDistance, false);
    }

    Bitap64(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        patternLength = pattern.length();
        if (patternLength > 64)
            throw new IllegalArgumentException("Pattern length exceeded allowed maximum in 64 characters");
        this.pattern = pattern;
        this.maxLevenshteinDistance = maxLevenshteinDistance;
        this.caseInsensitive = caseInsensitive;
        lastBitMask = 1L << (patternLength - 1);
        positionBitMasks = new Char2LongOpenHashMap(patternLength << 1);
        if (!caseInsensitive) {
            for (int i = 0; i < patternLength; i++) {
                final char c = pattern.charAt(i);
                positionBitMasks.put(c, positionBitMasks.getOrDefault(c, -1L) & (~(1L << i)));
            }
        } else {
            for (int i = 0; i < patternLength; i++) {
                final char lc = Character.toLowerCase(pattern.charAt(i));
                final long mask = positionBitMasks.getOrDefault(lc, -1L) & (~(1L << i));
                positionBitMasks.put(lc, mask);
                positionBitMasks.put(Character.toUpperCase(lc), mask);
            }
        }
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

    @Override
    public IterativeFuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex) {
        return new Matcher(text, fromIndex, toIndex);
    }

    final class Matcher implements IterativeFuzzyMatcher {
        private CharSequence text;
        private final int[] lengthChanges;
        private final int[] lengthChangesCopy;
        private long[] previousMatchings;
        private long[] currentMatchings;
        private int levenshteinDistance;
        private int maxDistance;
        private int index;
        private int maxIndex;

        private Matcher(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            maxDistance = maxLevenshteinDistance;
            final int n = maxDistance + 1;
            currentMatchings = new long[n];
            previousMatchings = new long[n];
            lengthChanges = new int[n];
            lengthChangesCopy = new int[n];
            index = Math.max(0, fromIndex) - 1;
            maxIndex = Math.min(text.length(), toIndex);
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
        }

        @Override
        public boolean find() {
            resetState();

            while (++index < maxIndex) {
                if (testNextSymbol()) {
                    int _maxDistance = maxDistance;
                    improveResult(Math.min(index + patternLength, maxIndex));
                    maxDistance = _maxDistance;
                    return true;
                }
            }

            //insert at the end
            for (int appendCount = 1; appendCount <= maxDistance; appendCount++) {
                if (testNextInsert(appendCount)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        @SuppressWarnings("ManualArrayCopy")
        public void improveResult(final int maxIndex) {
            if (levenshteinDistance == 0)
                return;
            if (index + 1 == maxIndex)
                return;
            //store
            int _index = index;
            int _levenshteinDistance = levenshteinDistance;
            for (int i = 1; i < _levenshteinDistance; i++) lengthChangesCopy[i] = lengthChanges[i];

            maxDistance = levenshteinDistance - 1;
            while (++index < maxIndex) {
                if(testNextSymbol()) {
                    improveResult(maxIndex);
                    return;
                }
            }

            //restore
            index = _index;
            levenshteinDistance = _levenshteinDistance;
            for (int i = 1; i < _levenshteinDistance; i++) lengthChanges[i] = lengthChangesCopy[i];
        }

        @Override
        public void setIndex(int index) {
            this.index = index;
        }

        @Override
        public void resetState() {
            for (int i = 1; i <= maxDistance; i++) lengthChanges[i] = 0;
            for (int i = 0; i <= maxDistance; i++) currentMatchings[i] = -1L;
        }

        @Override
        public int getMaxDistance() {
            return maxDistance;
        }

        @Override
        public void setMaxDistance(int maxDistance) {
            this.maxDistance = maxDistance;
        }

        @Override
        public boolean testNextSymbol() {
            final long charPositions = positionBitMasks.getOrDefault(text.charAt(index), -1L);
            swapMatching();
            levenshteinDistance = 0;
            currentMatchings[0] = (previousMatchings[0] << 1) | charPositions;
            if (0L == (currentMatchings[0] & lastBitMask)) {
                return true;
            }
            while (levenshteinDistance < maxDistance) {
                final long current = currentMatchings[levenshteinDistance];
                // ignore current character
                final long deletion = previousMatchings[levenshteinDistance++];
                // replace current character with correct one
                final long substitution = deletion << 1;
                // insertion of missing correct character before current position
                final long insertion = (deletion << 2) | charPositions;

                final long matching = (previousMatchings[levenshteinDistance] << 1) | charPositions;
                currentMatchings[levenshteinDistance] = insertion & deletion & substitution & matching;
                final boolean found = 0L == (currentMatchings[levenshteinDistance] & lastBitMask);
                if (current >= deletion) {
                    if (insertion < substitution && insertion < matching) {
                        lengthChanges[levenshteinDistance] = 1;
                    } else if (substitution < matching && deletion < current) {
                        lengthChanges[levenshteinDistance] = 0;
                        if (found) {
                            //try to change replacement of last character onto deletion of current if next is correct
                            final int nextIndex = index + 1;
                            if (nextIndex < maxIndex) {
                                final long nextCharPositions = positionBitMasks.getOrDefault(text.charAt(nextIndex), -1);
                                if ((nextCharPositions & lastBitMask) == 0L) {
                                    index = nextIndex;
                                    lengthChanges[levenshteinDistance] = -1;
                                }
                            }
                        }
                    } else if(matching < substitution) {
                        if (-1L == (matching | (~previousMatchings[levenshteinDistance]))) {
                            lengthChanges[levenshteinDistance] = -1;
                        }
                    }
                }
                if (found) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean testNextInsert(final int iteration) {
            final long bitMask = lastBitMask >>> iteration;
            final int limit = maxDistance - iteration;
            for (levenshteinDistance = 0; levenshteinDistance <= limit; levenshteinDistance++) {
                if (0L == (currentMatchings[levenshteinDistance] & bitMask)) {
                    index--;
                    final int end = levenshteinDistance + iteration;
                    for (int i = levenshteinDistance + 1; i <= end; i++) lengthChanges[i] = 1;
                    levenshteinDistance = end;
                    return true;
                }
            }
            return false;
        }

        private void swapMatching() {
            final long[] tmp = currentMatchings;
            currentMatchings = previousMatchings;
            previousMatchings = tmp;
        }

        @Override
        public int start() {
            int lengthChange = 0;
            for (int i = 1; i <= levenshteinDistance; i++) lengthChange += lengthChanges[i];
            return end() - patternLength + lengthChange;
        }

        @Override
        public int end() {
            if (index == -1)
                throw new IllegalStateException("find method must be called before index");
            if (index >= maxIndex)
                throw new IllegalStateException("no matches were found, last call of the find method returned false");
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

        @Override
        public FuzzyPattern pattern() {
            return Bitap64.this;
        }
    }

}