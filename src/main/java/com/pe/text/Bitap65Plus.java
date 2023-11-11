package com.pe.text;

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;

class Bitap65Plus extends BaseBitap {

    private final Char2ObjectOpenHashMap<BitVector> positionBitMasks;

    private final int patternLength;

    Bitap65Plus(CharSequence pattern, int maxLevenshteinDistance) {
        this(pattern, maxLevenshteinDistance, false);
    }

    Bitap65Plus(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        super(pattern, maxLevenshteinDistance, caseInsensitive);
        this.patternLength = pattern.length();
        this.positionBitMasks = new Char2ObjectOpenHashMap<>(pattern.length() << 1);
        if (!caseInsensitive) {
            for (int i = 0; i < this.patternLength; i++) {
                final char c = pattern.charAt(i);
                this.positionBitMasks.computeIfAbsent(c, k -> new BitVector(this.patternLength).setMinusOne())
                        .setZeroAt(i);
            }
        } else {
            for (int i = 0; i < this.patternLength; i++) {
                final char lc = Character.toLowerCase(pattern.charAt(i));
                final BitVector mask = this.positionBitMasks.computeIfAbsent(lc, k -> new BitVector(this.patternLength).setMinusOne())
                        .setZeroAt(i);
                this.positionBitMasks.put(Character.toUpperCase(lc), mask);
            }
        }
    }

    @Override
    public IterativeFuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex) {
        return new Matcher(text, fromIndex, toIndex);
    }

    final class Matcher extends BaseBitap.Matcher {

        private final BitVector substitution = new BitVector(Bitap65Plus.this.patternLength);
        private final BitVector insertion = new BitVector(Bitap65Plus.this.patternLength);
        private final BitVector matching = new BitVector(Bitap65Plus.this.patternLength);
        private final BitVector testDeletion = new BitVector(Bitap65Plus.this.patternLength);

        private BitVector[] previousMatchings;
        private BitVector[] currentMatchings;

        private Matcher(CharSequence text, int fromIndex, int toIndex) {
            super(text, fromIndex, toIndex);
            this.currentMatchings = createVectors(super.maxDistance + 1);
            this.previousMatchings = createVectors(super.maxDistance + 1);
        }

        private BitVector[] createVectors(int n) {
            BitVector[] result = new BitVector[n];
            for (int i = 0; i < n; i++) result[i] = new BitVector(patternLength);
            return result;
        }

        @Override
        public void resetState() {
            super.resetState();
            for (int i = 0; i <= maxDistance; i++) currentMatchings[i].setMinusOne();
        }

        @Override
        public boolean testNextSymbol() {
            final BitVector charPositions = Bitap65Plus.this.positionBitMasks.getOrDefault(super.text.charAt(super.index), null);
            swapMatching();
            super.levenshteinDistance = 0;
            if (charPositions == null) {
                this.currentMatchings[0].setMinusOne();
            } else {
                this.currentMatchings[0].setBitsFrom(this.previousMatchings[0])
                        .leftShift1().or(charPositions);
            }
            if (this.currentMatchings[0].hasZeroAtLastBit()) {
                return true;
            }
            while (super.levenshteinDistance < super.maxDistance) {
                final BitVector current = this.currentMatchings[super.levenshteinDistance];
                // ignore current character
                final BitVector deletion = this.previousMatchings[super.levenshteinDistance++];
                // replace current character with correct one
                this.substitution.setBitsFrom(deletion).leftShift1();
                // insertion of missing correct character before current position
                if (charPositions == null) {
                    this.insertion.setMinusOne();
                } else {
                    this.insertion.setBitsFrom(this.substitution).leftShift1().or(charPositions);
                }

                if (charPositions == null) {
                    this.matching.setMinusOne();
                } else {
                    this.matching.setBitsFrom(this.previousMatchings[super.levenshteinDistance])
                            .leftShift1().or(charPositions);
                }
                this.currentMatchings[super.levenshteinDistance].setBitsFrom(this.insertion)
                        .and(deletion).and(this.substitution).and(this.matching);
                final boolean found = this.currentMatchings[super.levenshteinDistance].hasZeroAtLastBit();
                if (!current.lessThan(deletion)) {
                    if (this.insertion.lessThan(this.substitution) && this.insertion.lessThan(this.matching)) {
                        super.lengthChanges[super.levenshteinDistance] = 1;
                    } else if (this.substitution.lessThan(this.matching) && deletion.lessThan(current)) {
                        super.lengthChanges[super.levenshteinDistance] = 0;
                        if (found) {
                            //try to change a replacement of the last character to a deletion of the current one if the next character does match
                            final int nextIndex = super.index + 1;
                            if (nextIndex < super.toIndex) {
                                final BitVector nextCharPositions = Bitap65Plus.this.positionBitMasks.get(super.text.charAt(nextIndex));
                                if (nextCharPositions != null && nextCharPositions.hasZeroAtLastBit()) {
                                    super.index = nextIndex;
                                    super.lengthChanges[super.levenshteinDistance] = -1;
                                }
                            }
                        }
                    } else if (this.matching.lessThan(this.substitution)) {
                        final boolean wasDeleted = this.testDeletion
                                .setBitsFrom(this.previousMatchings[super.levenshteinDistance])
                                .invert()
                                .or(this.matching)
                                .isMinusOne();
                        if (wasDeleted) {
                            super.lengthChanges[super.levenshteinDistance] = -1;
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
            final int limit = super.maxDistance - iteration;
            for (super.levenshteinDistance = 0; super.levenshteinDistance <= limit; super.levenshteinDistance++) {
                if (this.currentMatchings[super.levenshteinDistance].leftShift1().hasZeroAtLastBit()) {
                    final int end = super.levenshteinDistance + iteration;
                    for (int i = super.levenshteinDistance + 1; i <= end; i++) super.lengthChanges[i] = 1;
                    super.levenshteinDistance = end;
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
    }

}