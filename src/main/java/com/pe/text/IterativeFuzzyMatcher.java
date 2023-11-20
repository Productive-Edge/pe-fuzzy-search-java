package com.pe.text;

import java.util.Optional;

/**
 * This internal interface was extracted to implement {@link IterativeMultiplePatterns}'s which scans input char-by-char
 * by all patterns together. {@link FuzzyMatcher} interface is not enough to implement this approach,
 * since it requires to manage internal state each matcher and suspend on each character.
 */
interface IterativeFuzzyMatcher extends DefaultFuzzyMatcher {
    /**
     * Resets internal state of the Bitap matcher (bits masks of current findings, length changes, but not current position)
     * to continue search from the last successful matching
     */
    void resetState();

    /**
     * Method was extracted to allow MultiplePattern make single sequential {@link #text()} scan for its patterns
     *
     * @return true if next position has matching with pattern
     */
    boolean testNextSymbol();

    /**
     * Check if the matching can be found at the ond of text by appending characters from pattern (i.e. INSERT operation)
     *
     * @param iteration current iteration (i.e. amount of appended symbols)
     * @return {@code true} if the next match was found, otherwise - {@code false}.
     */
    boolean testNextInsert(int iteration);

    /**
     * Improve region of matching, to prioritize REPLACEMENT over DELETION.
     *
     * @param maxIndex maximum allowed end position to expand region
     */
    void improveResult(int maxIndex);

    /**
     * Updates current position of the matcher,
     * used in the {@link FuzzyMultiPattern} implementation to synchronize position of all patterns
     *
     * @param index - new position for search
     */
    void setIndex(int index);

    @Override
    default Optional<FuzzyResult> findTheBest(boolean includeOverlapped) {
        final int maxDistance = getMaxDistance();
        FuzzyResultRecord best = null;
        while (this.find()) {
            best = new FuzzyResultRecord(this);
            if (best.distance() == 0) {
                break;
            }
            this.setMaxDistance(best.distance() - 1);
            if (includeOverlapped) {
                this.reset(this.text(), this.start() + 1, this.to());
            }
        }
        this.setMaxDistance(maxDistance);
        return Optional.ofNullable(best);
    }

    /**
     * Returns current maximal allowed Levenshtein distance (it can be changed in process of searching the best matching)
     *
     * @return current maximal allowed Levenshtein distance
     */
    int getMaxDistance();

    /**
     * Changes current maximal allowed Levenshtein distance, for faster detection of the best matching.
     * This values has to be restored into the original after matching is found.
     *
     * @param maxDistance new maximal allowed Levenshtein distance, for faster detection of the best matching
     */
    void setMaxDistance(int maxDistance);
}
