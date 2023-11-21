package com.pe.text;

import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap;

class Bitap32 extends BaseBitap {

    private final Char2IntOpenHashMap positionBitMasks;
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
        positionBitMasks = new Char2IntOpenHashMap(pattern.length() << 1);
        if (!caseInsensitive) {
            for (int i = 0; i < pattern.length(); i++) {
                final char c = pattern.charAt(i);
                positionBitMasks.put(c, positionBitMasks.getOrDefault(c, -1) & (~(1 << i)));
            }
        } else {
            for (int i = 0; i < pattern.length(); i++) {
                final char lc = Character.toLowerCase(pattern.charAt(i));
                final int mask = positionBitMasks.getOrDefault(lc, -1) & (~(1 << i));
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
        private int[] previousMatchings;
        private int[] currentMatchings;

        private Matcher(CharSequence text, int fromIndex, int toIndex) {
            super(text, fromIndex, toIndex);
            this.currentMatchings = new int[super.maxDistance + 1];
            this.previousMatchings = new int[super.maxDistance + 1];
        }

        @Override
        public void resetState() {
            super.resetState();
            for (int i = 0; i <= super.maxDistance; i++) this.currentMatchings[i] = -1;
        }

        @Override
        public boolean testNextSymbol() {
            final int charPositions = Bitap32.this.positionBitMasks.getOrDefault(super.text.charAt(super.index), -1);
            swapMatching();
            super.levenshteinDistance = 0;
            this.currentMatchings[0] = (this.previousMatchings[0] << 1) | charPositions;
            if (0 == (this.currentMatchings[0] & Bitap32.this.lastBitMask)) {
                return true;
            }
            while (super.levenshteinDistance < super.maxDistance) {
                final int current = this.currentMatchings[super.levenshteinDistance];
                // delete current character
                final int deletion = this.previousMatchings[super.levenshteinDistance++];
                // replace current character with correct one
                final int substitution = deletion << 1;
                // insert correct character after the current
                final int insertion = current << 1;
                // get current character as is
                final int matching = (this.previousMatchings[super.levenshteinDistance] << 1) | charPositions;
                final int combined = this.currentMatchings[super.levenshteinDistance] = insertion & deletion & substitution & matching;
                final boolean found = 0 == (combined & Bitap32.this.lastBitMask);
                if (current > deletion) {
                    if (substitution < matching) {
                        // replacement or deletion
                        super.lengthChanges[super.levenshteinDistance] = 0;
//                        super.lengthChanges[super.levenshteinDistance]--;
                    }
                    //otherwise skip matched
                } else {
                    if (insertion < substitution && insertion < matching && matching < 0) {
                        //insert operation
                        super.lengthChanges[super.levenshteinDistance] = 1;
                    } else if (matching <= substitution) {
                        //matching operation
//                        if (matching < this.previousMatchings[super.levenshteinDistance]) {
                        if (-1 == (matching | (~this.previousMatchings[super.levenshteinDistance]))) {
                            //insert -> replacement or replacement -> deletion
                            super.lengthChanges[super.levenshteinDistance]--;
                        }/* else if (0 == (charPositions & (Bitap32.this.lastBitMask | 1))) {
                            super.lengthChanges[super.levenshteinDistance]--;
                        }*/
                    }
                }
                if (found) {
//                    if (((Bitap32.this.lastBitMask << 1) & combined) == 0) {
//                        this.lengthChanges[1] = 0;
//                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean testNextInsert(final int iteration) {
            throw new IllegalStateException();
        }

        private void swapMatching() {
            final int[] tmp = this.currentMatchings;
            this.currentMatchings = this.previousMatchings;
            this.previousMatchings = tmp;
        }

    }

}