package by.gena.juzzy;

import it.unimi.dsi.fastutil.chars.Char2LongMap;
import it.unimi.dsi.fastutil.chars.Char2LongOpenHashMap;

final class Bitap64 implements JuzzyPattern, IterativeJuzzyPattern {

    private final Char2LongMap positionBitMasks;
    private final CharSequence pattern;
    private final int patternLength;
    private final long lastBitMask;
    private final int maxLevenshteinDistance;
    private final boolean caseInsensitive;

    Bitap64(CharSequence pattern, int maxLevenshteinDistance) {
        this(pattern, maxLevenshteinDistance, false);
    }

    Bitap64(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        patternLength = pattern.length();
        if (patternLength > 64)
            throw new IllegalArgumentException("Pattern length exceeded allowed maximum in 64 characters");
        this.pattern = pattern;
        this.maxLevenshteinDistance = maxLevenshteinDistance;
        this.caseInsensitive = caseInsensitive;
        lastBitMask = 1L << (patternLength - 1);
        positionBitMasks = new Char2LongOpenHashMap(patternLength << 1);
        if (!caseInsensitive) {
            for (int i = 0; i < patternLength; i++) {
                char c = pattern.charAt(i);
                positionBitMasks.put(c, positionBitMasks.getOrDefault(c, -1L) & (~(1L << i)));
            }
        } else {
            for (int i = 0; i < patternLength; i++) {
                char lc = Character.toLowerCase(pattern.charAt(i));
                long mask = positionBitMasks.getOrDefault(lc, -1L) & (~(1L << i));
                positionBitMasks.put(lc, mask);
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
        private long[] previousMatchings;
        private long[] currentMatchings;
        private final int[] lengthChanges;
        private int levenshteinDistance;
        private int end;
        private final int textLength;

        private Matcher(CharSequence text) {
            this.text = text;
            currentMatchings = new long[maxLevenshteinDistance + 1];
            previousMatchings = new long[currentMatchings.length];
            lengthChanges = new int[currentMatchings.length];
            textLength = text.length();
            setEnd(-1);
        }

        @Override
        public void setEnd(final int fromIndex) {
            end = fromIndex;
        }

        @Override
        public boolean find(final int fromIndex) {
            setEnd(Math.max(0, fromIndex) - 1);
            return find();
        }

        public boolean find() {
            resetState();

            while (++end < textLength) {
                if (testNextSymbol())
                    return true;
            }

            //insert at the end
            for (int appendCount = 1; appendCount <= maxLevenshteinDistance; appendCount++) {
                if (testNextInsert(appendCount))
                    return true;
            }
            return false;
        }

        @Override
        public void resetState() {
            for (int i = 1, l = lengthChanges.length; i < l; i++) lengthChanges[i] = 0;
            for (int i = 0, l = currentMatchings.length; i < l; i++) currentMatchings[i] = -1L;
        }

        @Override
        public boolean testNextSymbol() {
            final long charPositions = positionBitMasks.getOrDefault(text.charAt(end), -1L);
            swapMatching();
            levenshteinDistance = 0;
            currentMatchings[0] = (previousMatchings[0] << 1) | charPositions;
            if (0L == (currentMatchings[0] & lastBitMask)) {
                return true;
            }
            while (levenshteinDistance < maxLevenshteinDistance) {
                // ignore current character
                final long deletion = previousMatchings[levenshteinDistance];
                // replace current character with correct one
                final long substitution = deletion << 1;
                // insertion of missing correct character before current position
                final long insertion = (substitution << 1) | charPositions;

                final int resultLengthChange = lengthChanges[levenshteinDistance];
                levenshteinDistance++;
                final long matching = (previousMatchings[levenshteinDistance] << 1) | charPositions;
                if (matching >= deletion) {
                    if (insertion < deletion) {
                        lengthChanges[levenshteinDistance] = resultLengthChange + 1;
                    }
                } else if (matching < substitution) {
                    if (-1L == (matching | (~previousMatchings[levenshteinDistance]))) {
                        //previous operation was deletion
                        lengthChanges[levenshteinDistance] = resultLengthChange - 1;
                    }
                }
                currentMatchings[levenshteinDistance] = insertion & deletion & substitution & matching;
                if (0L == (currentMatchings[levenshteinDistance] & lastBitMask)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean testNextInsert(final int iteration) {
            final long bitMask = lastBitMask >>> iteration;
            final int maxIndex = maxLevenshteinDistance - iteration;
            for (levenshteinDistance = 0; levenshteinDistance <= maxIndex; levenshteinDistance++) {
                if (0L == (currentMatchings[levenshteinDistance] & bitMask)) {
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
            final long[] tmp = currentMatchings;
            currentMatchings = previousMatchings;
            previousMatchings = tmp;
        }

        @Override
        public int start() {
            return end() - patternLength + lengthChanges[levenshteinDistance];
        }

        @Override
        public int end() {
            if (end == -1)
                throw new IllegalStateException("find method must be called before index");
            if (end >= textLength)
                throw new IllegalStateException("no matches were found, last call of the find method returned false");
            return end + 1;
        }

        @Override
        public int distance() {
            return levenshteinDistance;
        }

        @Override
        public CharSequence foundText() {
            return text.subSequence(start(), end());
        }

        @Override
        public JuzzyPattern pattern() {
            return Bitap64.this;
        }
    }

}