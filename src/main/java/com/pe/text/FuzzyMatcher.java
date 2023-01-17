package com.pe.text;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An engine that performs fuzzy match operations on a character sequence.
 *
 * A matcher is created from a pattern by invoking the pattern's {@link MatcherProvider#matcher}/{@link FuzzyPattern#matcher} methods:
 * <ul>
 *     <li>{@link MatcherProvider#matcher(CharSequence)} to create matcher for the input text</li>
 *     <li>{@link MatcherProvider#matcher(CharSequence, int)} to create matcher for the input text,
 *     to search from the specified index</li>
 *     <li>{@link MatcherProvider#matcher(CharSequence, int, int)} to create matcher for the input text,
 *       to searching in the specified bounds</li>
 * </ul>
 * Once created, a matcher can be used to perform following search operations:
 * <ul>
 *     <li>The {@link #find()} method scans the input sequence looking for the next subsequence that matches the pattern</li>
 *     <li>The {@link #findTheBestMatching()} method scans the input sequence looking for the best subsequence that matches the pattern</li>
 *     <li>The {@link #stream()}  method scans the input sequence and streams all subsequences that match the pattern</li>
 * </ul>
 *
 * The explicit state of a matcher is initially undefined; attempting to query any part of it before a successful match
 * will cause an {@link IllegalStateException} to be thrown. The explicit state of a matcher is recomputed
 * by every match operation.
 *
 * A matcher state may be reset explicitly by invoking its reset() methods.
 * Resetting a matcher discards its explicit state information.
 *
 * Instances of this class are not safe for use by multiple concurrent threads.
 */
public interface FuzzyMatcher extends FuzzyResult {

    /**
     * Returns the input text.
     * @return the input text.
     */
    CharSequence text();

    /**
     * Resets the internal state of the matcher, so the next call of {@link #find()}, {@link #findTheBestMatching()}, or
     * {@link #stream()} starts scanning from the first character in the input text up to the last one.
     */
    default void reset() {
        reset(text(), 0, text().length());
    }

    /**
     * Resets the internal state of the matcher, so the next call of {@link #find()}, {@link #findTheBestMatching()}, or
     * {@link #stream()} starts scanning from the specified offset in the input text up to the last character.
     *
     * @param fromIndex start of scanning for the next search operation.
     */
    default void reset(int fromIndex) {
        reset(text(), fromIndex, text().length());
    }

    /**
     * Resets the internal state of the matcher, so the next call of {@link #find()}, {@link #findTheBestMatching()}, or
     * {@link #stream()} starts scanning from the specified offset in the input text up to the specified last index.
     *
     * @param fromIndex start of scanning for the next search operation.
     * @param toIndex end of scanning (exclusive index) for the next search operation.
     */
    default void reset(int fromIndex, int toIndex) {
        reset(text(), fromIndex, toIndex);
    }

    /**
     * Resets the internal state of the matcher, so the next call of {@link #find()}, {@link #findTheBestMatching()}, or
     * {@link #stream()} starts scanning new specified input text from the first character in the input text up to the last one.
     *
     * @param text new text to scan.
     */
    default void reset(CharSequence text) {
        reset(text, 0, text.length());
    }

    /**
     * Resets the internal state of the matcher, so the next call of {@link #find()}, {@link #findTheBestMatching()}, or
     * {@link #stream()} starts scanning new text from the specified offset up to the last character.
     *
     * @param text new text to scan.
     * @param fromIndex start of scanning for the next search operation.
     */
    default void reset(CharSequence text, int fromIndex) {
        reset(text, fromIndex, text.length());
    }

    /**
     * Resets the internal state of the matcher, so the next call of {@link #find()}, {@link #findTheBestMatching()}, or
     * {@link #stream()} starts scanning new text from the specified offset up to the specified last index.
     *
     * @param text new text to scan.
     * @param fromIndex start of scanning for the next search operation.
     * @param toIndex end of scanning (exclusive index) for the next search operation.
     */
    void reset(CharSequence text, int fromIndex, int toIndex);

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
     * Attempts to find the next subsequence of the input sequence that matches the pattern.
     * This method starts at the beginning of this matcher's region, or,
     * if a previous invocation of the method was successful and the matcher has not since been reset,
     * at the first character not matched by the previous match (i.e. from the offset returned by {@link #end()} method).
     *
     * If the match succeeds then more information can be obtained via the {@link #start()}, {@link #end()},
     * {@link #distance()}, and {@link #foundText()} methods.
     *
     * @return true if, and only if, a subsequence of the input sequence matches this matcher's pattern.
     */
    boolean find();

    /**
     * Attempts to find the best match in the text. Result will be the first matching in the text in case there are
     * more than one matching with the minimal Levenshtein distance.
     *
     * It is faster equivalent to the
     * {@code matcher.stream().min(Comparator.comparingInt(FuzzyResult::distance))}
     *
     * @return the first best matching (i.e. with minimal Levenshtein distance) or {{@link Optional#empty()}} if no matches.
     */
    default Optional<FuzzyResult> findTheBestMatching() {
        FuzzyResultRecord best = null;
        if(this instanceof IterativeFuzzyMatcher) {
            while (find()) {
                best = new FuzzyResultRecord(this);
                if(best.distance() == 0)
                    break;
                ((IterativeFuzzyMatcher)this).setMaxDistance(best.distance() - 1);
            }
        } else {
            while (find()) {
                if(best == null || best.distance() > distance()) {
                    best = new FuzzyResultRecord(this);
                    if(distance() == 0)
                        break;
                }
            }
        }
        return Optional.ofNullable(best);
    }
}
