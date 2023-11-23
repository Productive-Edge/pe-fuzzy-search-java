package com.pe.text;

import it.unimi.dsi.fastutil.chars.Char2LongOpenHashMap;

class Bitap64 extends BaseBitap {

    private final Char2LongOpenHashMap positionBitMasks;
    private final long lastBitMask;

    Bitap64(CharSequence pattern, int maxLevenshteinDistance) {
        this(pattern, maxLevenshteinDistance, false);
    }

    Bitap64(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        super(pattern, maxLevenshteinDistance, caseInsensitive);
        if (pattern.length() > 64)
            throw new IllegalArgumentException("Pattern length exceeded allowed maximum in 64 characters");
        lastBitMask = 1L << (pattern.length() - 1);
        positionBitMasks = new Char2LongOpenHashMap(pattern.length() << 1);
        if (!caseInsensitive) {
            for (int i = 0; i < pattern.length(); i++) {
                final char c = pattern.charAt(i);
                positionBitMasks.put(c, positionBitMasks.getOrDefault(c, -1L) & (~(1L << i)));
            }
        } else {
            for (int i = 0; i < pattern.length(); i++) {
                final char lc = Character.toLowerCase(pattern.charAt(i));
                final long mask = positionBitMasks.getOrDefault(lc, -1L) & (~(1L << i));
                positionBitMasks.put(lc, mask);
                positionBitMasks.put(Character.toUpperCase(lc), mask);
            }
        }
    }

    @Override
    public IterativeFuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex) {
        return new Matcher(text, fromIndex, toIndex);
    }

    final class Matcher extends BaseBitap.Matcher {
        private long[] previousMatchings;
        private long[] currentMatchings;

        private Matcher(CharSequence text, int fromIndex, int toIndex) {
            super(text, fromIndex, toIndex);
            currentMatchings = new long[maxDistance + 1];
            previousMatchings = new long[maxDistance + 1];
        }

        @Override
        public void resetState() {
            super.resetState();
            long mask = -1L;
            for (int i = 0; i <= maxDistance; i++, mask <<= 1) currentMatchings[i] = mask;
        }

        @Override
        public boolean testNextSymbol() {
            final long charPositions = Bitap64.this.positionBitMasks.getOrDefault(text.charAt(index), -1L);
            swapMatching();
            levenshteinDistance = 0;
            currentMatchings[0] = (previousMatchings[0] << 1) | charPositions;
            if (0L == (currentMatchings[0] & Bitap64.this.lastBitMask)) {
                return true;
            }
            boolean applied = false;
            while (levenshteinDistance < maxDistance) {
                final long current = currentMatchings[levenshteinDistance];
                // ignore current character
                final long deletion = previousMatchings[levenshteinDistance++];
                // insert correct character after the current
                final long insertion = current << 1;
                // replace current character with correct one
                final long substitution = deletion << 1;
                final long previousMatching = previousMatchings[levenshteinDistance];
                // get current character as is
                final long matching = (previousMatching << 1) | charPositions;
                final long combined = currentMatchings[levenshteinDistance] = insertion & deletion & substitution & matching;
                final boolean found = 0L == (combined & Bitap64.this.lastBitMask);
                if (!applied) {
                    if (deletion < current) {
                        // skip previous operation
                        if (substitution < matching && lengthChanges[levenshteinDistance] == 1) {
                            lengthChanges[levenshteinDistance] = 0;
                            setInsertsAfter(levenshteinDistance);
                            applied = true;
                        }
                    } else {
                        if (matching <= insertion || matching >= 0L) {
                            // skip previous operation, otherwise transform it: replacement -> deletion | insert -> replacement (decrease length)
                            // matching < previousMatching
                            final long highBitDiff = ~(matching ^ previousMatching);
                            final long invert = (Long.MAX_VALUE | matching) ^ (Long.MAX_VALUE | previousMatching);
                            if ((invert < 0L) == (highBitDiff < previousMatching)) {
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
                                    && ((previousMatchings[maxDistance] & (~Bitap64.this.lastBitMask)) | charPositions) >= combined;
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

        private void swapMatching() {
            final long[] tmp = currentMatchings;
            currentMatchings = previousMatchings;
            previousMatchings = tmp;
        }

    }

}