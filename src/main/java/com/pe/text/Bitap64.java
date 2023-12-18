package com.pe.text;

import it.unimi.dsi.fastutil.chars.Char2LongOpenHashMap;

/**
 * Bitap implementation using 64-bit word.
 */
class Bitap64 extends BaseBitap {

    /**
     * Positions inverted bitmask for every character in the pattern
     */
    private final Char2LongOpenHashMap positionMasks;

    /**
     * Position bitmask (not inverted) of the last pattern character,
     * stores the stop condition for the matching
     */
    private final long lastBitMask;

    Bitap64(CharSequence pattern, int maxLevenshteinDistance) {
        this(pattern, maxLevenshteinDistance, false);
    }

    Bitap64(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        super(pattern, maxLevenshteinDistance, caseInsensitive);
        if (pattern.length() > 64)
            throw new IllegalArgumentException("Pattern length exceeded allowed maximum in 64 characters");
        lastBitMask = 1L << (pattern.length() - 1);
        positionMasks = new Char2LongOpenHashMap(pattern.length() << 1);
        if (!caseInsensitive) {
            for (int i = 0; i < pattern.length(); i++) {
                final char c = pattern.charAt(i);
                positionMasks.put(c, positionMasks.getOrDefault(c, -1L) & (~(1L << i)));
            }
        } else {
            for (int i = 0; i < pattern.length(); i++) {
                final char lc = Character.toLowerCase(pattern.charAt(i));
                final long mask = positionMasks.getOrDefault(lc, -1L) & (~(1L << i));
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

        private final long[][] matchings;
        private int matchingsIndex;

        private Matcher(CharSequence text, int fromIndex, int toIndex) {
            super(text, fromIndex, toIndex);
            matchings = new long[pattern().text().length() + maxDistance][maxDistance + 1];
        }

        @Override
        public void resetState() {
            matchingsIndex = 0;
            long mask = -1L;
            long[] first = matchings[0];
            for (int i = 0; i <= maxDistance; i++, mask <<= 1) first[i] = mask;
        }

        @Override
        public boolean testNextSymbol() {
            long charPositions = Bitap64.this.positionMasks.getOrDefault(text.charAt(index), -1L);
            long[] previous = matchings[matchingsIndex++];
            if (matchingsIndex == matchings.length) matchingsIndex = 0;
            long[] current = matchings[matchingsIndex];
            levenshteinDistance = 0;
            current[0] = (previous[0] << 1) | charPositions;
            if (0L == (current[0] & Bitap64.this.lastBitMask)) {
                if (lengthChanges.length > 1) lengthChanges[1] = 0;
                return true;
            }
            while (levenshteinDistance < maxDistance) {
                // insert correct character after the current
                final long insertion = current[levenshteinDistance] << 1;
                // delete current character
                long deletion = previous[levenshteinDistance++];
                // insert correct character after the current
                // replace current character with correct one
                long substitution = deletion << 1;
                // get current character as is
                long matching = (previous[levenshteinDistance] << 1) | charPositions;
                final long combined = current[levenshteinDistance] = insertion & deletion & substitution & matching;
                final boolean found = 0L == (combined & Bitap64.this.lastBitMask);
                if (found) {
                    if (levenshteinDistance < maxDistance) lengthChanges[levenshteinDistance + 1] = 0;
                    int reverseLevensteinDistance = levenshteinDistance;
                    int reverseMatchingsIndex = (matchingsIndex == 0 ? matchings.length : matchingsIndex) - 1;
                    int reverseIndex = index;
                    long reverseLastBitMask = Bitap64.this.lastBitMask;
                    do {
                        boolean inserted = false;
                        if ((matching & reverseLastBitMask) == 0L) {
                            reverseLastBitMask >>>= 1;
                        } else if ((deletion & reverseLastBitMask) == 0L) {
                            lengthChanges[reverseLevensteinDistance--] = -1;
                        } else if (((deletion << 1) & reverseLastBitMask) == 0L) {
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
                                charPositions = Bitap64.this.positionMasks.getOrDefault(text.charAt(--reverseIndex), -1L);
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