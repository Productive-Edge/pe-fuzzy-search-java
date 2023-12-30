package com.pe.text;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An engine that performs fuzzy match operations on a character sequence.
 * <p>
 * A matcher is created from a pattern by invoking the pattern's {@link FuzzyMatcherProvider#matcher}/{@link FuzzyPattern#matcher} methods:
 * <ul>
 *     <li>{@link FuzzyMatcherProvider#matcher(CharSequence)} to create matcher for the input text</li>
 *     <li>{@link FuzzyMatcherProvider#matcher(CharSequence, int)} to create matcher for the input text,
 *     to search from the specified index</li>
 *     <li>{@link FuzzyMatcherProvider#matcher(CharSequence, int, int)} to create matcher for the input text,
 *       to searching in the specified bounds</li>
 * </ul>
 * Once created, a matcher can be used to perform following search operations:
 * <ul>
 *     <li>The {@link #find()} method scans the input sequence looking for the next subsequence that matches the pattern</li>
 *     <li>The {@link #findTheBest()} method scans the input sequence looking for the best subsequence that matches the pattern</li>
 *     <li>The {@link #stream()}  method scans the input sequence and streams all subsequences that match the pattern</li>
 * </ul>
 * <p>
 * The explicit state of a matcher is initially undefined; attempting to query any part of it before a successful match
 * will cause an {@link IllegalStateException} to be thrown. The explicit state of a matcher is recomputed
 * by every match operation.
 * <p>
 * A matcher state may be reset explicitly by invoking its reset() methods.
 * Resetting a matcher discards its explicit state information.
 * <p>
 * Instances of this class are not safe for use by multiple concurrent threads.
 */
public interface FuzzyMatcher extends FuzzyResult {

    /**
     * Resets the internal state of the matcher, so the next call of {@link #find()}, {@link #findTheBest()}, or
     * {@link #stream()} starts scanning from the first character in the input text up to the last one.
     */
    default void reset() {
        reset(text(), 0, text().length());
    }

    /**
     * Resets the internal state of the matcher, so the next call of {@link #find()}, {@link #findTheBest()}, or
     * {@link #stream()} starts scanning new text from the specified offset up to the specified last index.
     *
     * @param text      new text to scan.
     * @param fromIndex start of scanning for the next search operation.
     * @param toIndex   end of scanning (exclusive index) for the next search operation.
     */
    void reset(CharSequence text, int fromIndex, int toIndex);

    /**
     * Returns the input text.
     *
     * @return the input text.
     */
    CharSequence text();

    /**
     * Resets the internal state of the matcher, so the next call of {@link #find()}, {@link #findTheBest()}, or
     * {@link #stream()} starts scanning from the specified offset in the input text up to the last character.
     *
     * @param fromIndex start of scanning for the next search operation.
     */
    default void reset(int fromIndex) {
        reset(text(), fromIndex, text().length());
    }

    /**
     * Resets the internal state of the matcher, so the next call of {@link #find()}, {@link #findTheBest()}, or
     * {@link #stream()} starts scanning from the specified offset in the input text up to the specified last index.
     *
     * @param fromIndex start of scanning for the next search operation.
     * @param toIndex   end of scanning (exclusive index) for the next search operation.
     */
    default void reset(int fromIndex, int toIndex) {
        reset(text(), fromIndex, toIndex);
    }

    /**
     * Resets the internal state of the matcher, so the next call of {@link #find()}, {@link #findTheBest()}, or
     * {@link #stream()} starts scanning new specified input text from the first character in the input text up to the last one.
     *
     * @param text new text to scan.
     */
    default void reset(CharSequence text) {
        reset(text, 0, text.length());
    }

    /**
     * Resets the internal state of the matcher, so the next call of {@link #find()}, {@link #findTheBest()}, or
     * {@link #stream()} starts scanning new text from the specified offset up to the last character.
     *
     * @param text      new text to scan.
     * @param fromIndex start of scanning for the next search operation.
     */
    default void reset(CharSequence text, int fromIndex) {
        reset(text, fromIndex, text.length());
    }

    /**
     * Streams all matches.
     * 2nd call of this method will return empty stream since matcher will be in the finished scanning state after the 1st call.
     * Use {@link #reset} methods to reset state of the matcher.
     *
     * @return stream with all matches.
     */
    default Stream<FuzzyResult> stream() {
        return StreamSupport.stream(() -> new FuzzyResultSpliterator(this),
                FuzzyResultSpliterator.CHARACTERISTICS, false);
    }

    /**
     * Attempts to find the best match in the text. Result will be the first matching in the text in case there are
     * more than one matching with the minimal Levenshtein distance, without overlapping matching
     * (i.e. next search will start from the end position of the previous matching).
     * <p>
     * This method is faster than
     * {@code matcher.stream().min(Comparator.comparingInt(FuzzyResult::distance))}
     *
     * @return the first best matching (i.e. with minimal Levenshtein distance) or {{@link Optional#empty()}} if no matches.
     * @see #findTheBest(boolean)
     */
    default Optional<FuzzyResult> findTheBest() {
        return findTheBest(false);
    }

    /**
     * Attempts to find the best match in the text. Result will be the first matching in the text in case there are
     * more than one matching with the minimal Levenshtein distance.
     * <p>
     * It is faster equivalent to the
     * {@code matcher.stream().min(Comparator.comparingInt(FuzzyResult::distance))}
     *
     * @param includeOverlapped if true next search will start from the (start + 1) position of the previous matching
     *                          to try to find better matching within range of previous one
     *                          (might be useful in rare specific where a long pattern consists of few repeating characters),
     *                          otherwise it will start from the end position of the previous matching (usual case for OCR-ed text)
     * @return the first best matching (i.e. with minimal Levenshtein distance) or {{@link Optional#empty()}} if no matches.
     */
    default Optional<FuzzyResult> findTheBest(boolean includeOverlapped) {
        FuzzyResultRecord best = null;
        while (this.find()) {
            if (best == null || best.distance() > this.distance()) {
                best = new FuzzyResultRecord(this);
                if (best.distance() == 0)
                    return Optional.of(best);
            }
            if (includeOverlapped) {
                this.reset(this.text(), this.start() + 1, this.to());
            }
        }
        return Optional.empty();
    }

    /**
     * Attempts to find the next subsequence of the input sequence that matches the pattern.
     * This method starts at the beginning of this matcher's region, or,
     * if a previous invocation of the method was successful and the matcher has not since been reset,
     * at the first character not matched by the previous match (i.e. from the offset returned by {@link #end()} method).
     * <p>
     * If the match succeeds then more information can be obtained via the {@link #start()}, {@link #end()},
     * {@link #distance()}, and {@link #foundText()} methods.
     *
     * @return {@code true} if the next match was found, otherwise - {@code false}.
     */
    boolean find();

    /**
     * Returns end of the search range, which can be changed via {@link #reset(CharSequence, int, int)}
     *
     * @return end of the search range (exclusive index, i.e. maximal can be {@link #text()}.length() )
     */
    int to();

    /**
     * Returns true if this matcher instance has search started, otherwise - false
     *
     * @return true if this matcher instance has search started, otherwise - false
     */
    boolean started();

    /**
     * Returns true if this matcher instance has search completed, otherwise - false
     *
     * @return true if this matcher instance has search completed, otherwise - false
     */
    boolean completed();

    /**
     * Returns start of the search range, which can be changed via {@link #reset(CharSequence, int, int)}
     *
     * @return start of the search range (inclusive index)
     */
    int from();
}
