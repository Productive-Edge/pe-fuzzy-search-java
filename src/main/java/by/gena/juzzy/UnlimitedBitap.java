package by.gena.juzzy;

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
        private final BitVector[] insertions;
        private final BitVector[] deletions;
        private final BitVector[] _insertions;
        private final BitVector[] _deletions;
        private BitVector[] previousMatchings;
        private BitVector[] currentMatchings;
        private int maxDistance;
        private int levenshteinDistance;
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
            insertions = createVectors(n);
            deletions = createVectors(n);
            _insertions = createVectors(n);
            _deletions = createVectors(n);
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
        public void improveResult(final int maxIndex) {
            if (levenshteinDistance == 0)
                return;
            //store
            int _index = index;
            int _levenshteinDistance = levenshteinDistance;
            for (int i = 1; i < _levenshteinDistance; i++) _deletions[i].setBitsFrom(deletions[i]);
            for (int i = 1; i < _levenshteinDistance; i++) _insertions[i].setBitsFrom(insertions[i]);

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
            for (int i = 1; i < _levenshteinDistance; i++) deletions[i].setBitsFrom(_deletions[i]);
            for (int i = 1; i < _levenshteinDistance; i++) insertions[i].setBitsFrom(_insertions[i]);
        }

        @Override
        public void resetState() {
            for (int i = 1; i <= maxDistance; i++) insertions[i].setMinusOne();
            for (int i = 1; i <= maxDistance; i++) deletions[i].setMinusOne();
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

                insertions[levenshteinDistance].leftShift1().or(1L);
                deletions[levenshteinDistance].leftShift1().or(1L);

                if (charPositions == null) {
                    matching.setMinusOne();
                } else {
                    matching.setBitsFrom(previousMatchings[levenshteinDistance])
                            .leftShift1().or(charPositions);
                }
                if(insertion.lessThan(substitution)) {
                    insertions[levenshteinDistance].and(-3L);
                }
                if(matching.lessThan(substitution)) {
                    final boolean wasDeleted = testDeletion
                            .setBitsFrom(previousMatchings[levenshteinDistance])
                            .invert()
                            .or(matching)
                            .isMinusOne();
                    if(wasDeleted) {
                        deletions[levenshteinDistance].and(-2L);
                    }
                }
                currentMatchings[levenshteinDistance].setBitsFrom(insertion)
                        .and(deletion).and(substitution).and(matching);
                if (currentMatchings[levenshteinDistance].hasZeroAtLastBit()) {
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
                    BitVector mask = new BitVector(patternLength);
                    for (int i = 1; i <= iteration; i++) {
                        final int offset = levenshteinDistance + i;
                        insertions[offset].leftShift(i);
                        mask.leftShift1().or(1L);
                        deletions[offset].leftShift(i).or(mask);
                    }
                    levenshteinDistance += iteration;
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

        private final BitVector edits = new BitVector(patternLength);
        private int countEdits(BitVector mask, final int length) {
            BitVector masked =  (patternLength & 63L) == 0L ? mask : edits.setMinusOne().leftShift(length).or(mask);
            return masked.isMinusOne() ? 0 : 1;
        }

        public int countOfInsertions(final int countOfDeletions) {
            final int length = patternLength - countOfDeletions;
            int result = 0;
            for (int i = 1; i <= levenshteinDistance; i++) result += countEdits(insertions[i], length);
            return result;
        }

        public int countOfDeletions() {
            int result = 0;
            for (int i = 1; i <= levenshteinDistance; i++) result += countEdits(deletions[i], patternLength);
            return result;
        }

        @Override
        public int start() {
            final int deleted = countOfDeletions();
            return end() - patternLength + countOfInsertions(deleted) - deleted;
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