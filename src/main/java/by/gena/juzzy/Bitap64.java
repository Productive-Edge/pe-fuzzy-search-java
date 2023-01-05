package by.gena.juzzy;

import it.unimi.dsi.fastutil.chars.Char2LongMap;
import it.unimi.dsi.fastutil.chars.Char2LongOpenHashMap;

class Bitap64 implements JuzzyPattern, IterativeJuzzyPattern {

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
    public JuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex) {
        return getIterativeMatcher(text, fromIndex, toIndex);
    }

    @Override
    public IterativeJuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex) {
        return new Matcher(text, fromIndex, toIndex);
    }

    final class Matcher implements IterativeJuzzyMatcher {
        private CharSequence text;
        private final long[] insertions;
        private final long[] deletions;
        private final long[] _insertions;
        private final long[] _deletions;
        private long[] previousMatchings;
        private long[] currentMatchings;
        private int maxDistance;
        private int levenshteinDistance;
        private int index;
        private int maxIndex;

        private Matcher(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            maxDistance = maxLevenshteinDistance;
            final int n = maxDistance + 1;
            currentMatchings = new long[n];
            previousMatchings = new long[n];
            insertions = new long[n];
            deletions = new long[n];
            _insertions = new long[n];
            _deletions = new long[n];
            index = Math.max(0, fromIndex) - 1;
            maxIndex = Math.min(text.length(), toIndex);
        }

        @Override
        public CharSequence text() {
            return text;
        }

        @Override
        public void reset(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            index = Math.max(0, fromIndex) - 1;
            maxIndex = Math.min(text.length(), maxIndex);
        }

        public boolean find() {
            resetState();

            while (++index < maxIndex) {
                if (testNextSymbol()) {
                    int _maxDistance = maxDistance;
                    improveResult(Math.min(index + patternLength, maxIndex));
                    maxDistance = _maxDistance;
                    return true;
                }
            }

            //insert at the end
            for (int appendCount = 1; appendCount <= maxDistance; appendCount++) {
                if (testNextInsert(appendCount)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        @SuppressWarnings("ManualArrayCopy")
        public void improveResult(final int maxIndex) {
            if (levenshteinDistance == 0)
                return;
            //store
            int _index = index;
            int _levenshteinDistance = levenshteinDistance;
            for (int i = 1; i < _levenshteinDistance; i++) _deletions[i] = deletions[i];
            for (int i = 1; i < _levenshteinDistance; i++) _insertions[i] = insertions[i];

            maxDistance = levenshteinDistance - 1;
            while (++index < maxIndex) {
                if(testNextSymbol()) {
                    improveResult(maxIndex);
                    return;
                }
            }

            //restore
            index = _index;
            levenshteinDistance = _levenshteinDistance;
            for (int i = 1; i < _levenshteinDistance; i++) deletions[i] = _deletions[i];
            for (int i = 1; i < _levenshteinDistance; i++) insertions[i] = _insertions[i];
        }

        @Override
        public void resetState() {
            for (int i = 1; i <= maxDistance; i++) insertions[i] = -1L;
            for (int i = 1; i <= maxDistance; i++) deletions[i] = -1L;
            for (int i = 0; i <= maxDistance; i++) currentMatchings[i] = -1L;
        }

        @Override
        public int getMaxDistance() {
            return maxDistance;
        }

        @Override
        public void setMaxDistance(int maxDistance) {
            this.maxDistance = maxDistance;
        }

        @Override
        public boolean testNextSymbol() {
            final long charPositions = positionBitMasks.getOrDefault(text.charAt(index), -1L);
            swapMatching();
            levenshteinDistance = 0;
            currentMatchings[0] = (previousMatchings[0] << 1) | charPositions;
            if (0L == (currentMatchings[0] & lastBitMask)) {
                return true;
            }
            while (levenshteinDistance < maxDistance) {
                // ignore current character
                final long deletion = previousMatchings[levenshteinDistance++];
                // replace current character with correct one
                final long substitution = deletion << 1;
                // insertion of missing correct character before current position
                final long insertion = (substitution << 1) | charPositions;

                insertions[levenshteinDistance] = (insertions[levenshteinDistance] << 1) | 1L;
                deletions[levenshteinDistance] = (deletions[levenshteinDistance] << 1) | 1L;
                final long matching = (previousMatchings[levenshteinDistance] << 1) | charPositions;
                if (insertion < substitution) {
                    insertions[levenshteinDistance] &= -3L;
                }
                if (matching < substitution) {
                    if (-1 == (matching | (~previousMatchings[levenshteinDistance]))) {
                        deletions[levenshteinDistance] &= -2L;
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
            final int limit = maxDistance - iteration;
            for (levenshteinDistance = 0; levenshteinDistance <= limit; levenshteinDistance++) {
                if (0L == (currentMatchings[levenshteinDistance] & bitMask)) {
                    index--;
                    for (int i = 1; i <= iteration; i++) {
                        final int offset = levenshteinDistance + i;
                        insertions[offset] <<= i;
                        deletions[offset] = (deletions[offset] << i) | ((1L << i) - 1L);
                    }
                    levenshteinDistance += iteration;
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

        private int countEdits(final long mask, final int length) {
            return (patternLength == 64 ? mask : mask | (-1L << length)) == -1 ? 0 : 1;
        }

        public int countOfInsertions(final int countOfDeletions) {
            final int length = patternLength - countOfDeletions;
            int result = 0;
            for (int i = 1; i <= levenshteinDistance; i++) result += countEdits(insertions[i], length);
            return result;
        }

        public int countOfDeletions() {
            int result = 0;
            for (int i = 1; i <= levenshteinDistance; i++) result += countEdits(deletions[i], patternLength);
            return result;
        }

        @Override
        public int start() {
            final int deleted = countOfDeletions();
            return end() - patternLength + countOfInsertions(deleted) - deleted;
        }

        @Override
        public int end() {
            if (index == -1)
                throw new IllegalStateException("find method must be called before index");
            if (index >= maxIndex)
                throw new IllegalStateException("no matches were found, last call of the find method returned false");
            return index + 1;
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