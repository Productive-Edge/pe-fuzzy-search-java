package com.pe.juzzy;

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;

class UnlimitedBitap implements JuzzyPattern, IterativeJuzzyPattern {

    private final Char2ObjectOpenHashMap<BitVector> positionBitMasks;
    private final CharSequence pattern;
    private final int patternLength;
    private final int maxLevenshteinDistance;
    private final boolean caseInsensitive;

    UnlimitedBitap(CharSequence pattern, int maxLevenshteinDistance) {
        this(pattern, maxLevenshteinDistance, false);
    }

    UnlimitedBitap(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        patternLength = pattern.length();
        this.pattern = pattern;
        this.maxLevenshteinDistance = maxLevenshteinDistance;
        this.caseInsensitive = caseInsensitive;
        positionBitMasks = new Char2ObjectOpenHashMap<>(patternLength << 1);
        if (!caseInsensitive) {
            for (int i = 0; i < patternLength; i++) {
                final char c = pattern.charAt(i);
                positionBitMasks.computeIfAbsent(c, k -> new BitVector(patternLength).setMinusOne())
                        .setZeroAt(i);
            }
        } else {
            for (int i = 0; i < patternLength; i++) {
                final char lc = Character.toLowerCase(pattern.charAt(i));
                final BitVector mask = positionBitMasks.computeIfAbsent(lc, k -> new BitVector(patternLength).setMinusOne())
                        .setZeroAt(i);
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
    public JuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex) {
        return getIterativeMatcher(text, fromIndex, toIndex);
    }

    @Override
    public IterativeJuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex) {
        return new Matcher(text, fromIndex, toIndex);
    }

    final class Matcher implements IterativeJuzzyMatcher {
        private CharSequence text;
        private final int[] lengthChanges;
        private final int[] lengthChangesCopy;
        private BitVector[] previousMatchings;
        private BitVector[] currentMatchings;
        private int levenshteinDistance;
        private int maxDistance;
        private int index;
        private int maxIndex;

        private final BitVector substitution = new BitVector(patternLength);
        private final BitVector insertion = new BitVector(patternLength);
        private final BitVector matching = new BitVector(patternLength);
        private final BitVector testDeletion = new BitVector(patternLength);

        private Matcher(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            maxDistance = maxLevenshteinDistance;
            final int n = maxDistance + 1;
            currentMatchings = createVectors(n);
            previousMatchings = createVectors(n);
            lengthChanges = new int[n];
            lengthChangesCopy = new int[n];
            index = Math.max(0, fromIndex) - 1;
            maxIndex = Math.min(text.length(), toIndex);
        }

        private BitVector[] createVectors(int n) {
            BitVector[] result = new BitVector[n];
            for (int i = 0; i < n; i++) result[i] = new BitVector(patternLength);
            return result;
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
                if (testNextSymbol()) {
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
            for (int i = 0; i <= maxDistance; i++) currentMatchings[i].setMinusOne();
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
            final BitVector charPositions = positionBitMasks.getOrDefault(text.charAt(index), null);
            swapMatching();
            levenshteinDistance = 0;
            if (charPositions == null) {
                currentMatchings[0].setMinusOne();
            } else {
                currentMatchings[0].setBitsFrom(previousMatchings[0])
                        .leftShift1().or(charPositions);
            }
            if (currentMatchings[0].hasZeroAtLastBit()) {
                return true;
            }
            while (levenshteinDistance < maxDistance) {
                final BitVector current = currentMatchings[levenshteinDistance];
                // ignore current character
                final BitVector deletion = previousMatchings[levenshteinDistance++];
                // replace current character with correct one
                substitution.setBitsFrom(deletion).leftShift1();
                // insertion of missing correct character before current position
                if (charPositions == null) {
                    insertion.setMinusOne();
                } else {
                    insertion.setBitsFrom(substitution).leftShift1().or(charPositions);
                }

                if (charPositions == null) {
                    matching.setMinusOne();
                } else {
                    matching.setBitsFrom(previousMatchings[levenshteinDistance])
                            .leftShift1().or(charPositions);
                }
                currentMatchings[levenshteinDistance].setBitsFrom(insertion)
                        .and(deletion).and(substitution).and(matching);
                final boolean found = currentMatchings[levenshteinDistance].hasZeroAtLastBit();
                if (!current.lessThan(deletion)) {
                    if(insertion.lessThan(substitution) && insertion.lessThan(matching)) {
                        lengthChanges[levenshteinDistance] = 1;
                    } else if (substitution.lessThan(matching) && deletion.lessThan(current)) {
                        lengthChanges[levenshteinDistance] = 0;
                        if (found) {
                            //try to change replacement of last character onto deletion of current if next is correct
                            final int nextIndex = index + 1;
                            if (nextIndex < maxIndex) {
                                final BitVector nextCharPositions = positionBitMasks.get(text.charAt(nextIndex));
                                if (nextCharPositions != null && nextCharPositions.hasZeroAtLastBit()) {
                                    index = nextIndex;
                                    lengthChanges[levenshteinDistance] = -1;
                                }
                            }
                        }
                    } else if(matching.lessThan(substitution)) {
                        final boolean wasDeleted = testDeletion
                                .setBitsFrom(previousMatchings[levenshteinDistance])
                                .invert()
                                .or(matching)
                                .isMinusOne();
                        if(wasDeleted) {
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
            final int limit = maxDistance - iteration;
            for (levenshteinDistance = 0; levenshteinDistance <= limit; levenshteinDistance++) {
                if (currentMatchings[levenshteinDistance].leftShift1().hasZeroAtLastBit()) {
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
            final BitVector[] tmp = currentMatchings;
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
        public JuzzyPattern pattern() {
            return UnlimitedBitap.this;
        }
    }

}