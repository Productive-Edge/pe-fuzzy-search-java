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
            this.currentMatchings = new long[super.maxDistance + 1];
            this.previousMatchings = new long[super.maxDistance + 1];
        }

        @Override
        public void resetState() {
            super.resetState();
            for (int i = 0; i <= maxDistance; i++) currentMatchings[i] = -1L;
        }

        @Override
        public boolean testNextSymbol() {
            final long charPositions = Bitap64.this.positionBitMasks.getOrDefault(super.text.charAt(super.index), -1L);
            swapMatching();
            super.levenshteinDistance = 0;
            this.currentMatchings[0] = (this.previousMatchings[0] << 1) | charPositions;
            if (0L == (this.currentMatchings[0] & Bitap64.this.lastBitMask)) {
                return true;
            }
            while (super.levenshteinDistance < super.maxDistance) {
                final long current = this.currentMatchings[super.levenshteinDistance];
                // ignore current character
                final long deletion = this.previousMatchings[super.levenshteinDistance++];
                // replace current character with correct one
                final long substitution = deletion << 1;
                // insertion of missing correct character before current position
                final long insertion = (deletion << 2) | charPositions;  // use out of order optimisation
//                final int insertion = currentMatchings[levenshteinDistance - 1] << 1; // original Bitap insert
                final long matching = (this.previousMatchings[super.levenshteinDistance] << 1) | charPositions;
                this.currentMatchings[super.levenshteinDistance] = insertion & deletion & substitution & matching;
                final boolean found = 0L == (this.currentMatchings[super.levenshteinDistance] & Bitap64.this.lastBitMask);
                if (current >= deletion) {
                    if (insertion < substitution && insertion < matching) {
                        super.lengthChanges[super.levenshteinDistance] = 1;
                    } else if (substitution < matching && deletion < current) {
                        super.lengthChanges[super.levenshteinDistance] = 0;
                        if (found) {
                            //try to change a replacement of the last character to a deletion of the current one if the next character does match
                            final int nextIndex = super.index + 1;
                            if (nextIndex < super.toIndex) {
                                final long nextCharPositions = Bitap64.this.positionBitMasks.getOrDefault(super.text.charAt(nextIndex), -1);
                                if ((nextCharPositions & Bitap64.this.lastBitMask) == 0L) {
                                    super.index = nextIndex;
                                    super.lengthChanges[super.levenshteinDistance] = -1;
                                }
                            }
                        }
                    } else if (matching < substitution) {
                        if (-1L == (matching | (~this.previousMatchings[super.levenshteinDistance]))) {
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
            final long bitMask = Bitap64.this.lastBitMask >>> iteration;
            final int limit = super.maxDistance - iteration;
            for (super.levenshteinDistance = 0; super.levenshteinDistance <= limit; super.levenshteinDistance++) {
                if (0L == (this.currentMatchings[super.levenshteinDistance] & bitMask)) {
                    final int end = super.levenshteinDistance + iteration;
                    for (int i = super.levenshteinDistance + 1; i <= end; i++) super.lengthChanges[i] = 1;
                    super.levenshteinDistance = end;
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