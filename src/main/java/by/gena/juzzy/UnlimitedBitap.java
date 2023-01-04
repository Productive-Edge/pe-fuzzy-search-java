package by.gena.juzzy;

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;

final class UnlimitedBitap implements JuzzyPattern, IterativeJuzzyPattern {

    private final Char2ObjectOpenHashMap<BitVector> positionBitMasks;
    private final CharSequence pattern;
    private final int patternLength;
    private final int maxLevenshteinDistance;
    private final boolean caseInsensitive;

    UnlimitedBitap(CharSequence pattern, int maxLevenshteinDistance) {
        this(pattern, maxLevenshteinDistance, false);
    }

    UnlimitedBitap(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        patternLength = pattern.length();
        this.pattern = pattern;
        this.maxLevenshteinDistance = maxLevenshteinDistance;
        this.caseInsensitive = caseInsensitive;
        positionBitMasks = new Char2ObjectOpenHashMap<>(patternLength << 1);
        if (!caseInsensitive) {
            for (int i = 0; i < patternLength; i++) {
                final char c = pattern.charAt(i);
                positionBitMasks.computeIfAbsent(c, k -> new BitVector(patternLength).setMinusOne())
                        .setZeroAt(i);
            }
        } else {
            for (int i = 0; i < patternLength; i++) {
                final char lc = Character.toLowerCase(pattern.charAt(i));
                final BitVector mask = positionBitMasks.computeIfAbsent(lc, k -> new BitVector(patternLength).setMinusOne())
                        .setZeroAt(i);
                positionBitMasks.put(Character.toUpperCase(lc), mask);
            }
        }
    }

    @Override
    public CharSequence text() {
        return pattern;
    }

    @Override
    public int maxLevenshteinDistance() {
        return maxLevenshteinDistance;
    }

    @Override
    public boolean caseInsensitive() {
        return caseInsensitive;
    }

    @Override
    public JuzzyMatcher matcher(CharSequence text) {
        return getIterativeMatcher(text);
    }

    @Override
    public IterativeJuzzyMatcher getIterativeMatcher(CharSequence text) {
        return new Matcher(text);
    }

    final class Matcher implements IterativeJuzzyMatcher {
        private final CharSequence text;
        private BitVector[] previousMatchings;
        private BitVector[] currentMatchings;
        private final int[] lengthChanges;
        private int levenshteinDistance;
        private int end;
        private final int textLength;

        private final BitVector substitution = new BitVector(patternLength);
        private final BitVector insertion = new BitVector(patternLength);
        private final BitVector matching = new BitVector(patternLength);
        private final BitVector inverted = new BitVector(patternLength);

        private Matcher(CharSequence text) {
            this.text = text;
            currentMatchings = new BitVector[maxLevenshteinDistance + 1];
            for (int i = 0; i < currentMatchings.length; i++) currentMatchings[i] = new BitVector(patternLength);
            previousMatchings = new BitVector[currentMatchings.length];
            for (int i = 0; i < previousMatchings.length; i++) previousMatchings[i] = new BitVector(patternLength);
            lengthChanges = new int[currentMatchings.length];
            textLength = text.length();
            setEnd(-1);
        }

        @Override
        public void setEnd(final int fromIndex) {
            end = fromIndex;
        }

        public boolean find() {
            resetState();

            while (++end < textLength) {
                if(testNextSymbol())
                    return true;
            }

            //insert at the end
            for (int appendCount = 1; appendCount <= maxLevenshteinDistance; appendCount++) {
                if(testNextInsert(appendCount))
                    return true;
            }
            return false;
        }

        @Override
        public void resetState() {
            for (int i = 0, l = lengthChanges.length; i < l; i++) lengthChanges[i] = 0;
            for(final BitVector vector : currentMatchings) vector.setMinusOne();
        }

        @Override
        public boolean testNextSymbol() {
            final BitVector charPositions = positionBitMasks.getOrDefault(text.charAt(end), null);
            swapMatching();
            levenshteinDistance = 0;
            if (charPositions == null) {
                currentMatchings[0].setMinusOne();
            } else {
                currentMatchings[0].setBitsFrom(previousMatchings[0])
                        .leftShift1().or(charPositions);
            }
            if (currentMatchings[0].hasZeroAtLastBit()) {
                return true;
            }
            while (levenshteinDistance < maxLevenshteinDistance) {
                // ignore current character
                final BitVector deletion = previousMatchings[levenshteinDistance];
                // replace current character with correct one
                substitution.setBitsFrom(deletion).leftShift1();
                // insertion of missing correct character before current position
                if (charPositions == null) {
                    insertion.setMinusOne();
                } else {
                    insertion.setBitsFrom(substitution).leftShift1().or(charPositions);
                }
                final int resultLengthChange = lengthChanges[levenshteinDistance];
                levenshteinDistance++;
                if (charPositions == null) {
                    matching.setMinusOne();
                } else {
                    matching.setBitsFrom(previousMatchings[levenshteinDistance])
                            .leftShift1().or(charPositions);
                }
                if (matching.greaterOrEqualThan(deletion)) {
                    if (!insertion.greaterOrEqualThan(deletion)) {
                        lengthChanges[levenshteinDistance] = resultLengthChange + 1;
                    }
                } else if (!matching.greaterOrEqualThan(substitution)) {
                    final boolean everyBitIsOne = inverted.setBitsFrom(previousMatchings[levenshteinDistance])
                            .invert().or(matching).isMinusOne();
                    if (everyBitIsOne) {
                        //previous operation was deletion
                        lengthChanges[levenshteinDistance] = resultLengthChange - 1;
                    }
                }
                currentMatchings[levenshteinDistance].setBitsFrom(insertion)
                        .and(deletion).and(substitution).and(matching);
                if (currentMatchings[levenshteinDistance].hasZeroAtLastBit()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean testNextInsert(final int iteration) {
            final int maxIndex = maxLevenshteinDistance - iteration;
            for (levenshteinDistance = 0; levenshteinDistance <= maxIndex; levenshteinDistance++) {
                if (currentMatchings[levenshteinDistance].leftShift1().hasZeroAtLastBit()) {
                    end--;
                    final int resultLengthChange = lengthChanges[levenshteinDistance];
                    levenshteinDistance += iteration;
                    lengthChanges[levenshteinDistance] = resultLengthChange + iteration;
                    return true;
                }
            }
            return false;
        }

        private void swapMatching() {
            final BitVector[] tmp = currentMatchings;
            currentMatchings = previousMatchings;
            previousMatchings = tmp;
        }

        public boolean find(int fromIndex) {
            setEnd(Math.max(0, fromIndex) - 1);
            return find();
        }

        public int start() {
            return end() - patternLength + lengthChanges[levenshteinDistance];
        }

        public int end() {
            if (end == -1)
                throw new IllegalStateException("find method must be called before index");
            if (end >= textLength)
                throw new IllegalStateException("no matches were found, last call of find return false");
            return end + 1;
        }

        public int distance() {
            return levenshteinDistance;
        }

        public CharSequence foundText() {
            return text.subSequence(start(), end());
        }

        public JuzzyPattern pattern() {
            return UnlimitedBitap.this;
        }

    }

    static final class BitVector {
        final long[] bits;
        final long lastBitMask;

        BitVector(final int length) {
            bits = new long[((length - 1) >>> 6) + 1];
            lastBitMask = 1L << ((length - 1) & 63L);
        }

        BitVector setZeroAt(final int bitIndex) {
            bits[bitIndex >>> 6] &= ~(1L << (bitIndex & 63));
            return this;
        }

        boolean hasZeroAtLastBit() {
            return 0L == (bits[bits.length - 1] & lastBitMask);
        }

        BitVector setMinusOne() {
    //        Arrays.fill(bits, value);
            for (int i = 0, l = bits.length; i < l; i++) bits[i] = -1L;
            return this;
        }

        BitVector setBitsFrom(final BitVector vector) {
    //        System.arraycopy(vector.bits, 0, bits, 0, bits.length); // is not effective! for short arrays
            for (int i = 0, l = bits.length; i < l; i++) bits[i] = vector.bits[i];
            return this;
        }

        BitVector or(final BitVector vector) {
            for (int i = 0, l = bits.length; i < l; i++) bits[i] |= vector.bits[i];
            return this;
        }

        BitVector and(final BitVector vector) {
            for (int i = 0, l = bits.length; i < l; i++) bits[i] &= vector.bits[i];
            return this;
        }

        BitVector invert() {
            for (int i = 0, l = bits.length; i < l; i++) bits[i] = ~bits[i];
            return this;
        }

        BitVector leftShift1() {
            long bit = 0;
            for (int i = 0, l = bits.length; i < l; i++) {
                final long overflow = bits[i] >>> 63;
                bits[i] <<= 1;
                bits[i] |= bit;
                bit = overflow;
            }
            return this;
        }

        boolean greaterOrEqualThan(final BitVector vector) {
            for (int i = bits.length - 1; i >= 0; i--) {
                final long delta = bits[i] - vector.bits[i];
                if (delta == 0L) continue;
                return delta > 0L;
            }
            return true;
        }

        boolean isMinusOne() {
            for (final long bit : bits) {
                if (bit != -1L) return false;
            }
            return true;
        }
    }
}