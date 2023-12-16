package com.pe.text;

import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap;

import java.util.Arrays;
import java.util.stream.Collectors;

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
//
//    static int roundUpInverted(int bits) {
//        // bits++;
//        bits &= bits >> 1;
//        bits &= bits >> 2;
//        bits &= bits >> 4;
//        bits &= bits >> 8;
//        bits &= bits >> 16;
//        return bits - 1;
//    }

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
            super.resetState();
            matchingsIndex = 0;
            int mask = -1;
            int[] first = matchings[0];
            for (int i = 0; i <= maxDistance; i++, mask <<= 1) first[i] = mask;
        }

        @Override
        public boolean testNextSymbol() {
            int charPositions = Bitap32.this.positionMasks.getOrDefault(text.charAt(index), -1);
            System.out.println("  \"" + text.subSequence(from(), index + 1) + '\"');
            int[] previous = matchings[matchingsIndex++];
            if (matchingsIndex == matchings.length) matchingsIndex = 0;
            int[] current = matchings[matchingsIndex];
            levenshteinDistance = 0;
            current[0] = (previous[0] << 1) | charPositions;
            System.out.println(text.charAt(index) + ": " + Integer.toBinaryString(current[0]));
            System.out.println("0: " + Integer.toBinaryString(current[0]) + " <- " + Integer.toBinaryString(previous[0]));
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
                System.out.println("L: " + levenshteinDistance);
                System.out.println("I: " + Integer.toBinaryString(insertion) + " <- " + Integer.toBinaryString(current[levenshteinDistance - 1]));
                System.out.println("S: " + Integer.toBinaryString(substitution) + " <- " + Integer.toBinaryString(deletion));
                System.out.println("M: " + Integer.toBinaryString(matching) + " <- " + Integer.toBinaryString(previous[levenshteinDistance]));
                System.out.println("C: " + Integer.toBinaryString(combined));
                final boolean found = 0 == (combined & Bitap32.this.lastBitMask);
                if (found) {
                    if (levenshteinDistance < maxDistance) lengthChanges[levenshteinDistance + 1] = 0;
                    int leviDist = levenshteinDistance;
                    int matchIndex = (matchingsIndex == 0 ? matchings.length : matchingsIndex) - 1;
                    int charIndex = index;
                    matching = combined | charPositions;
                    int dt = 0;
                    OperationType co = OperationType.INSERTION;
                    do {
                        final OperationType po = co;
                        co = (insertion < substitution || insertion > 0)
                                ? ((matching < insertion || matching > 0) ? OperationType.MATCHING : OperationType.INSERTION)
                                : ((matching < substitution || matching > 0) ? OperationType.MATCHING : OperationType.REPLACEMENT);
                        switch (co) {
                            case INSERTION: {
                                if (dt < 0) {
                                    int lengthChange = previous[leviDist] < matchings[matchIndex - dt][leviDist - 1] ? -1 : 0;
                                    do {
                                        lengthChanges[leviDist--] = lengthChange;
                                    } while (++dt < 0);
                                } else {
                                    lengthChanges[leviDist--] = 1;
                                }
                                break;
                            }
                            case REPLACEMENT: {
                                if (dt < 0) {
//                                    while (matching < -1) {
//                                        charPositions = Bitap32.this.positionMasks.getOrDefault(text.charAt(--charIndex), -1);
//                                        matchIndex = (matchIndex == 0 ? matchings.length : matchIndex) - 1;
//                                        current = previous;
//                                        previous = matchings[matchIndex];
//                                        matching = current[leviDist] | charPositions;
//                                    }
                                    do {
                                        lengthChanges[leviDist--] = -1;
                                    } while (++dt < 0);
                                    if (current[leviDist] < 0 && current[leviDist] >= previous[leviDist]) {
                                        dt--;
                                    }
                                } else {
                                    lengthChanges[leviDist--] = 0;
                                }
                                break;
                            }
                            case MATCHING:
                                if (po != OperationType.MATCHING) dt = 0;
                                if (current[leviDist] < 0 && current[leviDist] >= previous[leviDist]) {
                                    dt--;
                                }
                                break;
                            default:
                                break;
                        }

                        if (leviDist == 0) {
                            System.out.println("LC: " + Arrays.stream(lengthChanges).skip(1).limit(levenshteinDistance)
                                    .mapToObj(String::valueOf).collect(Collectors.joining(",")));
                            debug();
                            return true;
                        }

                        if (co == OperationType.INSERTION) {
                        } else {
                            if (charIndex > from()) {
                                charPositions = Bitap32.this.positionMasks.getOrDefault(text.charAt(--charIndex), -1);
                                matchIndex = (matchIndex == 0 ? matchings.length : matchIndex) - 1;
                                current = previous;
                                previous = matchings[matchIndex];
                            } else {
                                // only insertions can be here
                                while (leviDist > 0) lengthChanges[leviDist--] = 1;
                                return true;
                            }
                        }

                        insertion = current[leviDist - 1] << 1;
                        substitution = previous[leviDist - 1] << 1;
                        matching = current[leviDist] | charPositions;
                    } while (true);
                }
            }
            return false;
        }

    }

}