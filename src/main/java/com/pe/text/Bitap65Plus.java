package com.pe.text;

import com.pe.hash.Char2ObjMap;

/**
 * Bitap implementation using unlimited {@link BitVector} for cases where pattern length is more than 64 characters.
 */
class Bitap65Plus extends BaseBitap {

    /**
     * Positions inverted bitmask for every character in the pattern
     */
    private final Char2ObjMap<BitVector> positionMasks;

    Bitap65Plus(CharSequence pattern, int maxLevenshteinDistance) {
        this(pattern, maxLevenshteinDistance, false);
    }

    Bitap65Plus(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        super(pattern, maxLevenshteinDistance, caseInsensitive);
        positionMasks = new Char2ObjMap<>(
                caseInsensitive
                        ? pattern.toString().toUpperCase() + pattern.toString().toLowerCase()
                        : pattern,
                BitVector.class,
                null);
        if (!caseInsensitive) {
            for (int i = 0; i < pattern.length(); i++) {
                final char c = pattern.charAt(i);
                positionMasks.computeIfAbsent(c, k -> new BitVector(pattern.length()).resetToMinusOne())
                        .setZeroAt(i);
            }
        } else {
            for (int i = 0; i < pattern.length(); i++) {
                final char lc = Character.toLowerCase(pattern.charAt(i));
                final BitVector mask = positionMasks.computeIfAbsent(lc, k -> new BitVector(pattern.length()).resetToMinusOne())
                        .setZeroAt(i);
                positionMasks.put(Character.toUpperCase(lc), mask);
            }
        }
    }

    @Override
    public IterativeFuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex) {
        return new Matcher(text, fromIndex, toIndex);
    }

    final class Matcher extends BaseBitap.Matcher {

        private final BitVector[][] matchings;
        private final BitVector reverseDeletion = new BitVector(Bitap65Plus.this.text().length());
        private final BitVector substitution = new BitVector(Bitap65Plus.this.text().length());
        private final BitVector matching = new BitVector(Bitap65Plus.this.text().length());
        private final BitVector reverseLastBitMask = new BitVector(Bitap65Plus.this.text().length());
        private int matchingsIndex;

        private Matcher(CharSequence text, int fromIndex, int toIndex) {
            super(text, fromIndex, toIndex);
            matchings = new BitVector[pattern().text().length() + maxDistance][maxDistance + 1];
            for (BitVector[] ms : matchings) {
                for (int j = 0; j < ms.length; j++)
                    ms[j] = new BitVector(Bitap65Plus.this.text().length());
            }
        }

        @Override
        public void resetState() {
            matchingsIndex = 0;
            BitVector[] first = matchings[0];
            for (int i = 0; i <= maxDistance; i++) first[i].resetToMinusOne().leftShift(i);
        }

        @Override
        public boolean testNextSymbol() {
            BitVector charPositions = Bitap65Plus.this.positionMasks.get(text.charAt(index));
            BitVector[] previous = matchings[matchingsIndex++];
            if (matchingsIndex == matchings.length) matchingsIndex = 0;
            BitVector[] current = matchings[matchingsIndex];
            levenshteinDistance = 0;
            if (charPositions == null) {
                current[0].resetToMinusOne();
            } else {
                current[0].setBitsFrom(previous[0])
                        .leftShift1().or(charPositions);
            }
            if (current[0].hasZeroAtTheLastBit()) {
                if (lengthChanges.length > 1) lengthChanges[1] = 0;
                return true;
            }
            while (levenshteinDistance < maxDistance) {
                // delete current character
                final BitVector deletion = previous[levenshteinDistance++];
                // replace current character with correct one
                substitution.setBitsFrom(deletion).leftShift1();
                //set combined as insertion
                final BitVector combined = current[levenshteinDistance]
                        .setBitsFrom(current[levenshteinDistance - 1]).leftShift1() // as insertion
                        .and(deletion)
                        .and(substitution);

                if (charPositions != null) {
                    // get current character as is
                    matching.setBitsFrom(previous[levenshteinDistance]).leftShift1().or(charPositions);
                    combined.and(matching);
                }
                final boolean found = combined.hasZeroAtTheLastBit();
                if (found) {
                    if (levenshteinDistance < maxDistance) lengthChanges[levenshteinDistance + 1] = 0;
                    int reverseLevensteinDistance = levenshteinDistance;
                    int reverseMatchingsIndex = (matchingsIndex == 0 ? matchings.length : matchingsIndex) - 1;
                    int reverseIndex = index;
                    reverseLastBitMask.resetToZero().setOneAt(Bitap65Plus.this.text().length() - 1);
                    reverseDeletion.setBitsFrom(deletion);
                    do {
                        boolean inserted = false;
                        if (charPositions != null && matching.and(reverseLastBitMask).isZero()) {
                            reverseLastBitMask.rightUnsignedShift1();
                        } else if (reverseDeletion.and(reverseLastBitMask).isZero()) {
                            lengthChanges[reverseLevensteinDistance--] = -1;
                        } else if (substitution.and(reverseLastBitMask).isZero()) {
                            lengthChanges[reverseLevensteinDistance--] = 0;
                            reverseLastBitMask.rightUnsignedShift1();
                        } else {
                            lengthChanges[reverseLevensteinDistance--] = 1;
                            reverseLastBitMask.rightUnsignedShift1();
                            inserted = true;
                        }

                        if (reverseLevensteinDistance == 0) {
                            return true;
                        }

                        if (!inserted) {
                            if (reverseIndex > from()) {
                                charPositions = Bitap65Plus.this.positionMasks.get(text.charAt(--reverseIndex));
                                reverseMatchingsIndex = (reverseMatchingsIndex == 0 ? matchings.length : reverseMatchingsIndex) - 1;
                                previous = matchings[reverseMatchingsIndex];
                            } else {
                                // only insertions can be here
                                while (reverseLevensteinDistance > 0) lengthChanges[reverseLevensteinDistance--] = 1;
                                return true;
                            }
                        }

                        reverseDeletion.setBitsFrom(previous[reverseLevensteinDistance - 1]);
                        substitution.setBitsFrom(reverseDeletion).leftShift1();
                        if (charPositions != null)
                            matching.setBitsFrom(previous[reverseLevensteinDistance]).leftShift1().or(charPositions);
                    } while (true);
                }
            }
            return false;
        }

    }

}