package by.gena.juzzy;

import it.unimi.dsi.fastutil.chars.Char2IntMap;
import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap;

class Bitap32 implements JuzzyPattern, IterativeJuzzyPattern {

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
    public JuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex) {
        return getIterativeMatcher(text, fromIndex, toIndex);
    }

    @Override
    public IterativeJuzzyMatcher getIterativeMatcher(CharSequence text, int fromIndex, int toIndex) {
        return new Matcher(text, fromIndex, toIndex);
    }

    final class Matcher implements IterativeJuzzyMatcher {
        private CharSequence text;
        private final int[] insertions;
        private final int[] deletions;
        private final int[] _insertions;
        private final int[] _deletions;
        private int[] previousMatchings;
        private int[] currentMatchings;
        private int levenshteinDistance;
        private int maxDistance;
        private int index;
        private int maxIndex;

        private Matcher(CharSequence text, int fromIndex, int toIndex) {
            this.text = text;
            maxDistance = maxLevenshteinDistance;
            final int n = maxDistance + 1;
            currentMatchings = new int[n];
            previousMatchings = new int[n];
            insertions = new int[n];
            deletions = new int[n];
            _insertions = new int[n];
            _deletions = new int[n];
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

        @Override
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
                if (testNextSymbol()) {
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
            for (int i = 1; i <= maxDistance; i++) insertions[i] = -1;
            for (int i = 1; i <= maxDistance; i++) deletions[i] = -1;
            for (int i = 0; i <= maxDistance; i++) currentMatchings[i] = -1;
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
            final int charPositions = positionBitMasks.getOrDefault(text.charAt(index), -1);
            swapMatching();
            levenshteinDistance = 0;
            currentMatchings[0] = (previousMatchings[0] << 1) | charPositions;
            if (0 == (currentMatchings[0] & lastBitMask)) {
                return true;
            }
            while (levenshteinDistance < maxDistance) {
                // ignore current character
                final int deletion = previousMatchings[levenshteinDistance++];
                // replace current character with correct one
                final int substitution = deletion << 1;
                // insertion of missing correct character before current position
                final int insertion = (substitution << 1) | charPositions;
//                final int insertion = currentMatchings[levenshteinDistance - 1] << 1; // original Bitap insert
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
            final int limit = maxDistance - iteration;
            for (levenshteinDistance = 0; levenshteinDistance <= limit; levenshteinDistance++) {
                if (0 == (currentMatchings[levenshteinDistance] & bitMask)) {
                    index--;
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

        private int countEdits(final int mask, final int length) {
            return (patternLength == 32 ? mask : mask | (-1 << length)) == -1 ? 0 : 1;
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
            return Bitap32.this;
        }
    }

}