package com.pe.text;

import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap;

/**
 * Bitap implementation using 32-bit word, it is even slightly faster on the 64-bit CPUs.
 */
class Bitap32 extends BaseBitap {

    /**
     * Positions inverted bitmask for every character in the pattern
     */
    private final Char2IntOpenHashMap positionMasks;

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
        positionMasks = new Char2IntOpenHashMap(pattern.length() << 1);
        if (!caseInsensitive) {
            for (int i = 0; i < pattern.length(); i++) {
                final char c = pattern.charAt(i);
                positionMasks.put(c, positionMasks.getOrDefault(c, -1) & (~(1 << i)));
            }
        } else {
            for (int i = 0; i < pattern.length(); i++) {
                final char lc = Character.toLowerCase(pattern.charAt(i));
                final int mask = positionMasks.getOrDefault(lc, -1) & (~(1 << i));
                positionMasks.put(lc, mask);
                positionMasks.put(Character.toUpperCase(lc), mask);
            }
        }
    }

    static int roundUpInverted(int bits) {
        // bits++;
        bits &= bits >> 1;
        bits &= bits >> 2;
        bits &= bits >> 4;
        bits &= bits >> 8;
        bits &= bits >> 16;
        return bits - 1;
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
            matchings = new int[pattern().text().length()][maxDistance + 1];
        }

        @Override
        public void resetState() {
            super.resetState();
            matchingsIndex = 0;
            int mask = -1;
            int[] first = matchings[0];
            for (int i = 0; i <= maxDistance; i++, mask <<= 1) first[i] = mask;
        }

        @Override
        public boolean testNextSymbol() {
            int charPositions = Bitap32.this.positionMasks.getOrDefault(text.charAt(index), -1);
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
                int insertion = current[levenshteinDistance] << 1;
                // delete current character
                final int deletion = previous[levenshteinDistance++];
                // replace current character with correct one
                int substitution = deletion << 1;
                // get current character as is
                int matching = (previous[levenshteinDistance] << 1) | charPositions;
                int combined = current[levenshteinDistance] = insertion & deletion & substitution & matching;
                final boolean found = 0 == (combined & Bitap32.this.lastBitMask);
                if (found) {
                    if (levenshteinDistance < maxDistance) lengthChanges[levenshteinDistance + 1] = 0;
                    int lb = Bitap32.this.lastBitMask << 1;
                    int ld = levenshteinDistance;
                    int mi = (matchingsIndex == 0 ? matchings.length : matchingsIndex) - 1;
                    int ci = index;
                    matching = combined | charPositions | -lb;
                    int rdt = 0;
                    int idt = 0;
                    do {
                        OperationType co = (insertion < substitution || insertion > 0)
                                ? ((matching < insertion || matching > 0) ? OperationType.MATCHING : OperationType.INSERTION)
                                : ((matching < substitution || matching > 0) ? OperationType.MATCHING : OperationType.REPLACEMENT);
                        switch (co) {
                            case INSERTION:
                                lengthChanges[ld] = 1 + idt;
                                ld--;
                                break;
                            case REPLACEMENT:
                                lengthChanges[ld] = rdt;
                                ld--;
                                break;
                            case MATCHING:
                                idt = rdt;
                                rdt = current[ld] >= previous[ld] ? -1 : 0;
                                break;
                            default:
                                break;
                        }
                        if (ld == 0)
                            return true;

                        if (co != OperationType.INSERTION || idt == -1) {
                            if (ci > from()) {
                                ci--;
                                if (rdt == 0) {
                                    lb >>= 1;
                                    if (lb == 0) lb = Integer.MAX_VALUE;
                                }
                                mi = (mi == 0 ? matchings.length : mi) - 1;
                                current = previous;
                                charPositions = Bitap32.this.positionMasks.getOrDefault(text.charAt(ci), -1);
                                previous = matchings[mi];
                            } else {
                                // only insertions can be here
                                while (ld > 0) lengthChanges[ld--] = 1;
                                return true;
                            }
                        }
                        insertion = current[ld - 1] << 1;
                        substitution = previous[ld - 1] << 1;
                        matching = current[ld] | charPositions | -lb;
                    } while (true);
                }
            }
            return false;
        }

    }

}