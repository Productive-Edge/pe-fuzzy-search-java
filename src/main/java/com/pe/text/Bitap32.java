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

    @Override
    public IterativeFuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex) {
        return new Matcher(text, fromIndex, toIndex);
    }
    
    final class Matcher extends BaseBitap.Matcher {

        /**
         * The best matching masks for each amount of edits at the previous index
         */
        private int[] previousMatchings;
        /**
         * The best matching masks for each amount of edits at the current index
         */
        private int[] currentMatchings;

        private Matcher(CharSequence text, int fromIndex, int toIndex) {
            super(text, fromIndex, toIndex);
            currentMatchings = new int[maxDistance + 1];
            previousMatchings = new int[maxDistance + 1];
        }

        @Override
        public void resetState() {
            super.resetState();
            for (int i = 0, mask = -1; i <= maxDistance; i++, mask <<= 1) currentMatchings[i] = mask;
        }

        @Override
        public boolean testNextSymbol() {
            final int charPositions = Bitap32.this.positionMasks.getOrDefault(text.charAt(index), -1);
            swapMatchings();
            levenshteinDistance = 0;
            currentMatchings[0] = (previousMatchings[0] << 1) | charPositions;
            if (0 == (currentMatchings[0] & Bitap32.this.lastBitMask)) {
                return true;
            }
            boolean applied = false;
            while (levenshteinDistance < maxDistance) {
                final int current = currentMatchings[levenshteinDistance];
                // delete current character
                final int deletion = previousMatchings[levenshteinDistance++];
                // insert correct character after the current
                final int insertion = current << 1;
                // replace current character with correct one
                final int substitution = deletion << 1;
                final int previousMatching = previousMatchings[levenshteinDistance];
                // get current character as is
                final int matching = (previousMatching << 1) | charPositions;
                final int combined = currentMatchings[levenshteinDistance] = insertion & deletion & substitution & matching;
                final boolean found = 0 == (combined & Bitap32.this.lastBitMask);
                if (!applied) {
                    if (deletion < current) {
                        // skip previous operation
                        if (substitution < matching && lengthChanges[levenshteinDistance] == 1) {
                            lengthChanges[levenshteinDistance] = 0;
                            setInsertsAfter(levenshteinDistance);
                            applied = true;
                        }
                    } else {
                        if (matching <= insertion || matching >= 0) {
                            // skip previous operation, otherwise transform it: replacement -> deletion | insert -> replacement (decrease length)
                            // matching < previousMatching
                            final int highBitDiff = ~(matching ^ previousMatching);
                            final int invert = (Integer.MAX_VALUE | matching) ^ (Integer.MAX_VALUE | previousMatching);
                            if ((invert < 0) == (highBitDiff < previousMatching)) {
                                lengthChanges[levenshteinDistance]--;
                                setInsertsAfter(levenshteinDistance);
                                applied = true;
                            }
                        } else if (current < deletion) { // most likely insertion
                            // skip it if character can match later
                            if (insertion < previousMatching && insertion <= ((previousMatchings[maxDistance] << 1) | charPositions)) {
                                lengthChanges[levenshteinDistance] = 1;
                                setInsertsAfter(levenshteinDistance);
                                applied = true;
                            }
                        } else {
                            // can be insertion or replacement
                            final boolean isReplacement = lengthChanges[levenshteinDistance] == 1
                                    && lengthChanges[maxDistance] != -1
                                    && ((previousMatchings[maxDistance] & (~Bitap32.this.lastBitMask)) | charPositions) >= combined;
                            if (isReplacement) {
                                lengthChanges[levenshteinDistance] = 0;
                                setInsertsAfter(levenshteinDistance);
                                applied = true;
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

        private void swapMatchings() {
            final int[] tmp = currentMatchings;
            currentMatchings = previousMatchings;
            previousMatchings = tmp;
        }

    }

}