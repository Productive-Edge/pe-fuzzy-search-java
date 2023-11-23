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
            for (int i = 0, mask = -1; i <= super.maxDistance; i++, mask <<= 1) this.currentMatchings[i] = mask;
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
            boolean applied = false;
            while (super.levenshteinDistance < super.maxDistance) {
                final int current = this.currentMatchings[super.levenshteinDistance];
                // delete current character
                final int deletion = this.previousMatchings[super.levenshteinDistance++];
                // insert correct character after the current
                final int insertion = current << 1;
                // replace current character with correct one
                final int substitution = deletion << 1;
                final int previousMatching = this.previousMatchings[super.levenshteinDistance];
                // get current character as is
                final int matching = (previousMatching << 1) | charPositions;
                final int combined = this.currentMatchings[super.levenshteinDistance] = insertion & deletion & substitution & matching;
                final boolean found = 0 == (combined & Bitap32.this.lastBitMask);
                if (!applied) {
                    if (deletion < current) {
                        // skip previous operation
                        if (substitution < matching && lengthChanges[levenshteinDistance] == 1) {
                            super.lengthChanges[super.levenshteinDistance] = 0;
                            setInsertsAfter(super.levenshteinDistance);
                            applied = true;
                        }
                    } else {
                        if (matching <= insertion || matching >= 0) {
                            // skip previous operation, otherwise transform it: replacement -> deletion | insert -> replacement (decrease length)
                            // matching < previousMatching
                            final int highBitDiff = ~(matching ^ previousMatching);
                            final int invert = (Integer.MAX_VALUE | matching) ^ (Integer.MAX_VALUE | previousMatching);
                            if ((invert < 0) == (highBitDiff < previousMatching)) {
                                super.lengthChanges[super.levenshteinDistance]--;
                                setInsertsAfter(super.levenshteinDistance);
                                applied = true;
                            }
                        } else if (current < deletion) { // most likely insertion
                            // skip it if character can match later
                            if (insertion <= ((previousMatchings[maxDistance] << 1) | charPositions)) {
                                if (insertion < previousMatchings[levenshteinDistance]) {
                                    super.lengthChanges[super.levenshteinDistance] = 1;
                                    setInsertsAfter(super.levenshteinDistance);
                                    applied = true;
                                }
                            }
                        } else {
                            final boolean isReplacement = super.lengthChanges[super.levenshteinDistance] == 1
                                    && ((previousMatchings[maxDistance] & (~Bitap32.this.lastBitMask)) | charPositions) >= combined;
                            // can be insertion or replacement
                            if (isReplacement && lengthChanges[maxDistance] != -1) {
                                super.lengthChanges[super.levenshteinDistance] = 0;
                                setInsertsAfter(super.levenshteinDistance);
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