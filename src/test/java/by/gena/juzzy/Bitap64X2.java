package by.gena.juzzy;

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;

import java.util.Arrays;

class Bitap64X2 implements JuzzyPattern {

    private final Char2ObjectOpenHashMap<BitVector2> positionBitMasks;
    private final CharSequence pattern;
    private final int patternLength;
    private final int maxLevenshteinDistance;
    private final boolean caseInsensitive;

    Bitap64X2(CharSequence pattern, int maxLevenshteinDistance) {
        this(pattern, maxLevenshteinDistance, false);
    }

    Bitap64X2(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        patternLength = pattern.length();
        this.pattern = pattern;
        this.maxLevenshteinDistance = maxLevenshteinDistance;
        this.caseInsensitive = caseInsensitive;
        positionBitMasks = new Char2ObjectOpenHashMap<>(patternLength << 1);
        if (!caseInsensitive) {
            for (int i = 0; i < patternLength; i++) {
                char c = pattern.charAt(i);
                positionBitMasks.computeIfAbsent(c, k -> new BitVector2(patternLength).fill(-1L))
                        .setZeroAt(i);
            }
        } else {
            for (int i = 0; i < patternLength; i++) {
                char lc = Character.toLowerCase(pattern.charAt(i));
                BitVector2 mask = positionBitMasks.computeIfAbsent(lc, k -> new BitVector2(patternLength).fill(-1L))
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

    public JuzzyMatcher matcher(CharSequence text) {
        return new Matcher(text);
    }

    class Matcher implements JuzzyMatcher {
        private final CharSequence text;
        private BitVector2[] previousMatchings;
        private BitVector2[] currentMatchings;
        private final int[] lengthChanges;
        private int levenshteinDistance;
        private int end;
        private final int textLength;

        private final BitVector2 substitution = new BitVector2(patternLength);
        private final BitVector2 insertion = new BitVector2(patternLength);
        private final BitVector2 matching = new BitVector2(patternLength);
        private final BitVector2 inverted = new BitVector2(patternLength);

        private Matcher(CharSequence text) {
            this.text = text;
            currentMatchings = new BitVector2[maxLevenshteinDistance + 1];
            for (int i = currentMatchings.length - 1; i >= 0; i--) currentMatchings[i] = new BitVector2(patternLength);
            previousMatchings = new BitVector2[currentMatchings.length];
            for (int i = previousMatchings.length - 1; i >= 0; i--) previousMatchings[i] = new BitVector2(patternLength);
            lengthChanges = new int[currentMatchings.length];
            textLength = text.length();
            init(0);
        }

        private void init(int fromIndex) {
            end = Math.max(0, fromIndex) - 1;
        }

        public boolean find() {
            Arrays.fill(lengthChanges, 0);
            for (int i = currentMatchings.length - 1; i >= 0; i--) currentMatchings[i].fill(-1L);
            while (++end < textLength) {
                BitVector2 charPositions = positionBitMasks.getOrDefault(text.charAt(end), null);
                swapMatching();
                levenshteinDistance = 0;
                if (charPositions == null) {
                    currentMatchings[0].fill(-1L);
                } else {
                    currentMatchings[0].setBitsFrom(previousMatchings[0])
                            .leftShift1().or(charPositions);
                }
                if (currentMatchings[0].hasZeroAtLastBit()) {
                    return true;
                }
                while (levenshteinDistance < maxLevenshteinDistance) {
                    // ignore current character
                    final BitVector2 deletion = previousMatchings[levenshteinDistance];
                    // replace current character with correct one
                    substitution.setBitsFrom(deletion).leftShift1();
                    // insertion of missing correct character before current position
                    if (charPositions == null) {
                        insertion.fill(-1L);
                    } else {
                        insertion.setBitsFrom(substitution).leftShift1().or(charPositions);
                    }
                    final int resultLengthChange = lengthChanges[levenshteinDistance];
                    levenshteinDistance++;
                    if (charPositions == null) {
                        matching.fill(-1L);
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
            }

            //insert at the end
            for (int appendCount = 1; appendCount <= maxLevenshteinDistance; appendCount++) {
                final int maxIndex = maxLevenshteinDistance - appendCount;
                for (levenshteinDistance = 0; levenshteinDistance <= maxIndex; levenshteinDistance++) {
                    if (currentMatchings[levenshteinDistance].leftShift1().hasZeroAtLastBit()) {
                        end--;
                        final int resultLengthChange = lengthChanges[levenshteinDistance];
                        levenshteinDistance += appendCount;
                        lengthChanges[levenshteinDistance] = resultLengthChange + appendCount;
                        return true;
                    }
                }
            }
            return false;
        }

        private void swapMatching() {
            final BitVector2[] tmp = currentMatchings;
            currentMatchings = previousMatchings;
            previousMatchings = tmp;
        }

        public final boolean find(int fromIndex) {
            init(fromIndex);
            return find();
        }

        public final int start() {
            return end() - patternLength + lengthChanges[levenshteinDistance];
        }

        public final int end() {
            if (end == -1)
                throw new IllegalStateException("find method must be called before index");
            if (end >= textLength)
                throw new IllegalStateException("no matches were found, last call of find return false");
            return end + 1;
        }

        public final int distance() {
            return levenshteinDistance;
        }

        public final CharSequence foundText() {
            return text.subSequence(start(), end());
        }

        public final JuzzyPattern pattern() {
            return Bitap64X2.this;
        }
    }

}