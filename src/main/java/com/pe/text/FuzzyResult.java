package com.pe.text;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Interface of the found matching.
 * <p>
 * {@link FuzzyMatcher} itself implements this interface to reduce heap memory usage
 * when search is done via {@link FuzzyMatcher#find()} method in the {@code while} loop:
 * <pre>{@code
 *     FuzzyMatcher matcher = pattern.matcher(text);
 *     while(matcher.find()) {
 *      System.out.println("Found text: " + matcher.foundText());
 *     }
 * }</pre>
 * <p>
 * Internal class {@link FuzzyResultRecord} also implements this interface to store matching results produced by
 * {@link FuzzyMatcher#stream()} and {@link FuzzyMatcher#findTheBest()}
 */
public interface FuzzyResult {

    /**
     * Returns the start index of the match.
     *
     * @return The index of the first character matched.
     * @throws IllegalStateException in case {@link FuzzyMatcher#start()} was called when no current matching was found.
     */
    int start();

    /**
     * Returns the offset after the last character matched.
     * <pre>{@code
     *     FuzzyMatcher matcher = pattern.matcher(text);
     *     while(matcher.find()) {
     *      System.out.println("Found text: " + text.substring(matcher.start(), matcher.end());
     *     }
     * }</pre>
     *
     * @return The offset after the last character matched.
     * @throws IllegalStateException in case {@link FuzzyMatcher#end()} was called when no current matching was found.
     */
    int end();

    /**
     * Returns the found subsequence in the input text.
     * For a matcher <code>m</code> with input sequence <code>s</code>, the expressions <code>m.foundText()</code> and <code>s.subSequence(m.start(), m.end())</code> are equivalent.
     *
     * @return The found subsequence in the input text.
     * @throws IllegalStateException in case {@link FuzzyMatcher#foundText()} was called when no current matching was found.
     */
    CharSequence foundText();

    /**
     * Returns similarity of the found subsequence with the pattern, which value is between 0.0f and 1.0f calculated as
     * {@code (pattern.length - distance) / pattern.length}
     *
     * @return similarity of the found subsequence with the pattern, which value is between 0.0f and 1.0f
     */
    default float similarity() {
        return (pattern().text().length() - distance()) / (float) pattern().text().length();
    }

    /**
     * The reference of the fuzzy search pattern was successfully matched.
     *
     * @return The reference of the fuzzy search pattern.
     */
    FuzzyPattern pattern();

    /**
     * Returns Levenshtein distance between the pattern text and found one.
     *
     * @return The Levenshtein distance between the pattern text and found one.
     * @throws IllegalStateException in case {@link FuzzyMatcher#distance()} was called when no current matching was found.
     */
    int distance();

    default Stream<Operation> streamEditsDetails() {
        return StreamSupport.stream(
                () -> new OperationsSpliterator(new OperationsIterator(this, false)),
                OperationsSpliterator.CHARACTERISTICS,
                false
        );
    }

    default void debug() {
        streamCharByCharOperations()
                .map(o -> {
                    switch (o.type()) {
                        case MATCHING:
                            return "[" + o.textChar().value() + '=' + o.patternChar().value() + ']';
                        case REPLACEMENT:
                            return "[" + o.textChar().value() + '>' + o.patternChar().value() + ']';
                        case INSERTION:
                            return "[+" + o.patternChar().value() + ']';
                        case DELETION:
                            return "[" + o.textChar().value() + "-]";
                        default:
                            return o.type().toString();
                    }
                })
                .forEach(System.out::print);
        System.out.println();
    }

    /**
     * Streams all operations for this result char-by-char including matchings.
     *
     * @return stream with all operations for this result char-by-char including matchings
     * @see Operation
     */
    default Stream<Operation> streamCharByCharOperations() {
        return StreamSupport.stream(
                () -> new OperationsSpliterator(new OperationsIterator(this, true)),
                OperationsSpliterator.CHARACTERISTICS,
                false
        );
    }

    /**
     * Streams edit operations applied to the found text to get pattern.
     *
     * @return stream with edit operations applied to the found text to get pattern
     * @see Operation
     */
    default Stream<Operation> streamEditOperations() {
        return StreamSupport.stream(
                () -> new OperationsSpliterator(new OperationsIterator(this, false)),
                OperationsSpliterator.CHARACTERISTICS,
                false
        );
    }

    /**
     * Streams edit operations, without matching, applied to the text to match the pattern
     *
     * @return stream with applied edit operations, without matching.
     */
    Stream<OperationType> streamEditTypes();
}