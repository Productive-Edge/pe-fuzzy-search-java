package by.gena.juzzy;

import it.unimi.dsi.fastutil.chars.Char2IntMap;
import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap;

final class Bitap32 implements JuzzyPattern, IterativeJuzzyPattern {

    private final Char2IntMap positionBitMasks;
    private final CharSequence pattern;
    private final int patternLength;
    private final int lastBitMask;
    private final int maxLevenshteinDistance;
    private final boolean caseInsensitive;

    public Bitap32(CharSequence pattern, int maxLevenshteinDistance) {
        this(pattern, maxLevenshteinDistance, false);
    }

    public Bitap32(CharSequence pattern, int maxLevenshteinDistance, boolean caseInsensitive) {
        patternLength = pattern.length();
        if (patternLength > 32) {
            throw new IllegalArgumentException("Pattern length exceeds allowed maximum in 32 characters");
        }
        this.pattern = pattern;
        this.maxLevenshteinDistance = maxLevenshteinDistance;
        this.caseInsensitive = caseInsensitive;
        lastBitMask = 1 << (patternLength - 1);
        positionBitMasks = new Char2IntOpenHashMap(patternLength << 1);
        if (!caseInsensitive) {
            for (int i = 0; i < patternLength; i++) {
                char c = pattern.charAt(i);
                positionBitMasks.put(c, positionBitMasks.getOrDefault(c, -1) & (~(1 << i)));
            }
        } else {
            for (int i = 0; i < patternLength; i++) {
                char lc = Character.toLowerCase(pattern.charAt(i));
                int mask = positionBitMasks.getOrDefault(lc, -1) & (~(1 << i));
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
        private int[] previousMatchings;
        private int[] currentMatchings;
        private final int[] insertions;
        private final int[] deletions;
        private int levenshteinDistance;
        private int end;
        private final int textLength;

        private Matcher(CharSequence text) {
            this.text = text;
            currentMatchings = new int[maxLevenshteinDistance + 1];
            previousMatchings = new int[currentMatchings.length];
            insertions = new int[currentMatchings.length];
            deletions = new int[currentMatchings.length];
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
            final int l = currentMatchings.length;
            for (int i = 1; i < l; i++) insertions[i] = -1;
            for (int i = 1; i < l; i++) deletions[i] = -1;
            for (int i = 0; i < l; i++) currentMatchings[i] = -1;
        }

        @Override
        public boolean testNextSymbol() {
            final int charPositions = positionBitMasks.getOrDefault(text.charAt(end), -1);
            swapMatching();
            levenshteinDistance = 0;
            currentMatchings[0] = (previousMatchings[0] << 1) | charPositions;
            if (0 == (currentMatchings[0] & lastBitMask)) {
                return true;
            }
            while (levenshteinDistance < maxLevenshteinDistance) {
//                final int insertion = currentMatchings[levenshteinDistance] << 1; // original Bitap insert
                // ignore current character
                final int deletion = previousMatchings[levenshteinDistance++];
                // replace current character with correct one
                final int substitution = deletion << 1;
//                // insertion of missing correct character before current position
                final int insertion = (substitution << 1) | charPositions;
                insertions[levenshteinDistance] = (insertions[levenshteinDistance] << 1) | 1;
                deletions[levenshteinDistance] = (deletions[levenshteinDistance] << 1) | 1;
                final int matching = (previousMatchings[levenshteinDistance] << 1) | charPositions;
                if (insertion < substitution) {
                    insertions[levenshteinDistance] &= -3;
                }
                if (matching < substitution) {
                    if (-1 == (matching | (~previousMatchings[levenshteinDistance]))) {
                        deletions[levenshteinDistance] &= -2;
                    }
                }
                currentMatchings[levenshteinDistance] = insertion & deletion & substitution & matching;
                if (0 == (currentMatchings[levenshteinDistance] & lastBitMask)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean testNextInsert(final int iteration) {
            final int bitMask = lastBitMask >>> iteration;
            final int maxIndex = maxLevenshteinDistance - iteration;
            for (levenshteinDistance = 0; levenshteinDistance <= maxIndex; levenshteinDistance++) {
                if (0 == (currentMatchings[levenshteinDistance] & bitMask)) {
                    end--;
                    for (int i = 1; i <= iteration; i++) {
                        final int offset = levenshteinDistance + i;
                        insertions[offset] <<= i;
                        deletions[offset] = (deletions[offset] << i) | ((1 << i) - 1);
                    }
                    levenshteinDistance += iteration;
                    return true;
                }
            }
            return false;
        }

        private void swapMatching() {
            final int[] tmp = currentMatchings;
            currentMatchings = previousMatchings;
            previousMatchings = tmp;
        }

        private int countEdits(int mask) {
            return (patternLength == 32 ? mask : mask | (-1 << patternLength)) == -1 ? 0 : 1;
        }

        public int countOfInsertions() {
            int result = 0;
            for (int i = 1; i <= levenshteinDistance; i++) result += countEdits(insertions[i]);
            return result;
        }

        public int countOfDeletions() {
            int result = 0;
            for (int i = 1; i <= levenshteinDistance; i++) result += countEdits(deletions[i]);
            return result;
        }

        @Override
        public int start() {
            return end() - patternLength + countOfInsertions() - countOfDeletions();
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
            return Bitap32.this;
        }
    }

}