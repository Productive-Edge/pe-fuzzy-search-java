package com.pe.text;

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;

class Bitap65Plus extends BaseBitap {

    private final Char2ObjectOpenHashMap<BitVector> positionBitMasks;

    Bitap65Plus(CharSequence pattern, int maxLevenshteinDistance) {
        this(pattern, maxLevenshteinDistance, false);
    }

    Bitap65Plus(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        super(pattern, maxLevenshteinDistance, caseInsensitive);
        positionBitMasks = new Char2ObjectOpenHashMap<>(pattern.length() << 1);
        if (!caseInsensitive) {
            for (int i = 0; i < pattern.length(); i++) {
                final char c = pattern.charAt(i);
                positionBitMasks.computeIfAbsent(c, k -> new BitVector(pattern.length()).setMinusOne())
                        .setZeroAt(i);
            }
        } else {
            for (int i = 0; i < pattern.length(); i++) {
                final char lc = Character.toLowerCase(pattern.charAt(i));
                final BitVector mask = positionBitMasks.computeIfAbsent(lc, k -> new BitVector(pattern.length()).setMinusOne())
                        .setZeroAt(i);
                positionBitMasks.put(Character.toUpperCase(lc), mask);
            }
        }
    }

    @Override
    public IterativeFuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex) {
        return new Matcher(text, fromIndex, toIndex);
    }

    final class Matcher extends BaseBitap.Matcher {

        private final BitVector substitution = new BitVector(Bitap65Plus.this.text().length());
        private final BitVector insertion = new BitVector(Bitap65Plus.this.text().length());
        private final BitVector matching = new BitVector(Bitap65Plus.this.text().length());
        private final BitVector highBitDiff = new BitVector(Bitap65Plus.this.text().length());
        private final BitVector previousMatchingMax = new BitVector(Bitap65Plus.this.text().length());

        private BitVector[] previousMatchings;
        private BitVector[] currentMatchings;

        private Matcher(CharSequence text, int fromIndex, int toIndex) {
            super(text, fromIndex, toIndex);
            currentMatchings = createVectors(maxDistance + 1);
            previousMatchings = createVectors(maxDistance + 1);
        }

        private BitVector[] createVectors(int n) {
            BitVector[] result = new BitVector[n];
            for (int i = 0; i < n; i++) result[i] = new BitVector(Bitap65Plus.this.text().length());
            return result;
        }

        @Override
        public void resetState() {
            super.resetState();
            for (int i = 0; i <= maxDistance; i++) currentMatchings[i].setMinusOne().leftShift(i);
        }

        @Override
        public boolean testNextSymbol() {
            final BitVector charPositions = Bitap65Plus.this.positionBitMasks.getOrDefault(text.charAt(index), null);
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
            boolean applied = false;
            while (levenshteinDistance < maxDistance) {
                final BitVector current = currentMatchings[levenshteinDistance];
                // ignore current character
                final BitVector deletion = previousMatchings[levenshteinDistance++];
                // insert correct character after the current
                insertion.setBitsFrom(current).leftShift1();
                // replace current character with correct one
                substitution.setBitsFrom(deletion).leftShift1();

                BitVector previousMatching = previousMatchings[levenshteinDistance];
                if (charPositions == null) {
                    matching.setMinusOne();
                } else {
                    matching.setBitsFrom(previousMatching)
                            .leftShift1().or(charPositions);
                }

                final BitVector combined = currentMatchings[levenshteinDistance].setBitsFrom(insertion)
                        .and(deletion).and(substitution).and(matching);

                final boolean found = combined.hasZeroAtLastBit();
                if (!applied) {
                    if (deletion.lessThan(current)) {
                        // skip previous operation
                        if (substitution.lessThan(matching) && lengthChanges[levenshteinDistance] == 1) {
                            lengthChanges[levenshteinDistance] = 0;
                            setInsertsAfter(levenshteinDistance);
                            applied = true;
                        }
                    } else {
                        if (insertion.notLessThan(matching) || matching.isPositive()) {
                            // skip previous operation, otherwise transform it: replacement -> deletion | insert -> replacement (decrease length)
                            // matching < previousMatching
                            highBitDiff.setBitsFrom(matching).xor(previousMatching).invert();
                            boolean invert = matching.isNegative() != previousMatching.isNegative();
                            if (invert == highBitDiff.lessThan(previousMatching)) {
                                lengthChanges[levenshteinDistance]--;
                                setInsertsAfter(levenshteinDistance);
                                applied = true;
                            }
                        } else if (current.lessThan(deletion)) { // most likely insertion
                            // skip it if character can match later
                            if (insertion.lessThan(previousMatching)) {
                                previousMatchingMax.setBitsFrom(previousMatchings[maxDistance]).leftShift1().or(charPositions);
                                if (previousMatchingMax.notLessThan(insertion)) {
                                    lengthChanges[levenshteinDistance] = 1;
                                    setInsertsAfter(levenshteinDistance);
                                    applied = true;
                                }
                            }
                        } else {
                            // can be insertion or replacement
                            if (lengthChanges[levenshteinDistance] == 1 && lengthChanges[maxDistance] != -1) {
                                final boolean isReplacement = charPositions == null
                                        || getMaxMatchingMask(charPositions).notLessThan(combined);
                                if (isReplacement) {
                                    lengthChanges[levenshteinDistance] = 0;
                                    setInsertsAfter(levenshteinDistance);
                                    applied = true;
                                }
                            }
                        }
                    }
                }
                if (found) {
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

        private BitVector getMaxMatchingMask(BitVector charPositions) {
            return previousMatchingMax
                    .setBitsFrom(previousMatchings[maxDistance])
                    .andInvertedLastBitMask()
                    .or(charPositions);
        }

    }

}