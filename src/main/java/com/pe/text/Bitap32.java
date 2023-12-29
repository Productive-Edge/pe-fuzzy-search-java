package com.pe.text;

import com.pe.hash.Char2IntMap;

/**
 * Bitap implementation using 32-bit word, it is even slightly faster on the 64-bit CPUs.
 */
class Bitap32 extends BaseBitap {

    /**
     * Positions inverted bitmask for every character in the pattern
     */
    private final Char2IntMap positionMasks;

    /**
     * Position bitmask (not inverted) of the last pattern character,
     * stores the stop condition for the matching
     */
    private final int lastBitMask;

    public Bitap32(CharSequence pattern, int maxLevenshteinDistance) {
        this(pattern, maxLevenshteinDistance, false);
    }

    public Bitap32(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        super(pattern, maxLevenshteinDistance, caseInsensitive);
        if (pattern.length() > 32) {
            throw new IllegalArgumentException("Pattern length exceeds allowed maximum in 32 characters");
        }
        lastBitMask = 1 << (pattern.length() - 1);
        positionMasks = new Char2IntMap(
                caseInsensitive
                        ? pattern.toString().toUpperCase() + pattern.toString().toLowerCase()
                        : pattern,
                -1);
        if (!caseInsensitive) {
            for (int i = 0; i < pattern.length(); i++) {
                final char c = pattern.charAt(i);
                positionMasks.put(c, positionMasks.get(c) & (~(1 << i)));
            }
        } else {
            for (int i = 0; i < pattern.length(); i++) {
                final char lc = Character.toLowerCase(pattern.charAt(i));
                final int mask = positionMasks.get(lc) & (~(1 << i));
                positionMasks.put(lc, mask);
                positionMasks.put(Character.toUpperCase(lc), mask);
            }
        }
    }

    @Override
    public IterativeFuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex) {
        return new Matcher(text, fromIndex, toIndex);
    }

    final class Matcher extends BaseBitap.Matcher {

        private final int[][] matchings;
        private int matchingsIndex;

        private Matcher(CharSequence text, int fromIndex, int toIndex) {
            super(text, fromIndex, toIndex);
            matchings = new int[pattern().text().length() + maxDistance][maxDistance + 1];
        }

        @Override
        public void resetState() {
            matchingsIndex = 0;
            int mask = -1;
            int[] first = matchings[0];
            for (int i = 0; i <= maxDistance; i++, mask <<= 1) first[i] = mask;
        }

        @Override
        public boolean testNextSymbol() {
            int charPositions = Bitap32.this.positionMasks.get(text.charAt(index));
            int[] previous = matchings[matchingsIndex++];
            if (matchingsIndex == matchings.length) matchingsIndex = 0;
            int[] current = matchings[matchingsIndex];
            levenshteinDistance = 0;
            current[0] = (previous[0] << 1) | charPositions;
            if (0 == (current[0] & Bitap32.this.lastBitMask)) {
                if (lengthChanges.length > 1) lengthChanges[1] = 0;
                return true;
            }
            while (levenshteinDistance < maxDistance) {
                // insert correct character after the current
                final int insertion = current[levenshteinDistance] << 1;
                // delete current character
                int deletion = previous[levenshteinDistance++];
                // replace current character with correct one
                int substitution = deletion << 1;
                // get current character as is
                int matching = (previous[levenshteinDistance] << 1) | charPositions;
                final int combined = current[levenshteinDistance] = insertion & deletion & substitution & matching;
                final boolean found = 0 == (combined & Bitap32.this.lastBitMask);
                if (found) {
                    if (levenshteinDistance < maxDistance) lengthChanges[levenshteinDistance + 1] = 0;
                    int reverseLevensteinDistance = levenshteinDistance;
                    int reverseMatchingsIndex = (matchingsIndex == 0 ? matchings.length : matchingsIndex) - 1;
                    int reverseIndex = index;
                    int reverseLastBitMask = Bitap32.this.lastBitMask;
                    do {
                        boolean inserted = false;
                        if ((matching & reverseLastBitMask) == 0) {
                            reverseLastBitMask >>>= 1;
                        } else if ((deletion & reverseLastBitMask) == 0) {
                            lengthChanges[reverseLevensteinDistance--] = -1;
                        } else if (((deletion << 1) & reverseLastBitMask) == 0) {
                            lengthChanges[reverseLevensteinDistance--] = 0;
                            reverseLastBitMask >>>= 1;
                        } else {
                            lengthChanges[reverseLevensteinDistance--] = 1;
                            reverseLastBitMask >>>= 1;
                            inserted = true;
                        }

                        if (reverseLevensteinDistance == 0) {
                            return true;
                        }

                        if (!inserted) {
                            if (reverseIndex > from()) {
                                charPositions = Bitap32.this.positionMasks.get(text.charAt(--reverseIndex));
                                reverseMatchingsIndex = (reverseMatchingsIndex == 0 ? matchings.length : reverseMatchingsIndex) - 1;
                                previous = matchings[reverseMatchingsIndex];
                            } else {
                                // only insertions can be here
                                while (reverseLevensteinDistance > 0) lengthChanges[reverseLevensteinDistance--] = 1;
                                return true;
                            }
                        }

                        deletion = previous[reverseLevensteinDistance - 1];
                        matching = (previous[reverseLevensteinDistance] << 1) | charPositions;
                    } while (true);
                }
            }
            return false;
        }

    }

}