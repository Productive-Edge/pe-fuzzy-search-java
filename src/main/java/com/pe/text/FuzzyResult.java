package com.pe.text;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Interface representing the result of a fuzzy text matching operation.
 * <p>
 * This interface provides methods to access information about a successful match, including:
 * <ul>
 *     <li>The start and end positions of the match in the input text</li>
 *     <li>The matched text itself</li>
 *     <li>The Levenshtein distance between the pattern and the matched text</li>
 *     <li>The similarity score between the pattern and the matched text</li>
 *     <li>Details about the edit operations that transform the matched text to the pattern</li>
 * </ul>
 * <p>
 * {@link FuzzyMatcher} itself implements this interface to reduce heap memory usage
 * when search is done via {@link FuzzyMatcher#find()} method in the {@code while} loop:
 * <pre>{@code
 *     FuzzyMatcher matcher = pattern.matcher(text);
 *     while(matcher.find()) {
 *         System.out.println("Found text: " + matcher.foundText());
 *         System.out.println("Levenshtein distance: " + matcher.distance());
 *         System.out.println("Similarity: " + matcher.similarity());
 *     }
 * }</pre>
 * <p>
 * Internal class {@link FuzzyResultRecord} also implements this interface to store matching results produced by
 * {@link FuzzyMatcher#stream()} and {@link FuzzyMatcher#findTheBest()}
 * <p>
 * <strong>Edge Cases:</strong>
 * <ul>
 *     <li>Most methods will throw {@link IllegalStateException} if called when no match has been found</li>
 *     <li>For exact matches, {@link #distance()} will return 0 and {@link #similarity()} will return 1.0</li>
 *     <li>For matches with maximum allowed Levenshtein distance, similarity will be at its minimum value</li>
 * </ul>
 *
 * @see FuzzyMatcher
 * @see FuzzyPattern
 * @see Operation
 * @see OperationType
 */
public interface FuzzyResult {

    /**
     * Returns the start index of the match in the input text.
     * <p>
     * This is the position of the first character of the matched subsequence in the original input text.
     * This index is zero-based.
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     *     FuzzyMatcher matcher = pattern.matcher("Hello World");
     *     if (matcher.find()) {
     *         int startPos = matcher.start(); // Get the starting position of the match
     *     }
     * }</pre>
     *
     * @return The index of the first character matched.
     * @throws IllegalStateException if called when no match has been found.
     * @see #end()
     * @see #foundText()
     */
    int start();

    /**
     * Returns the offset after the last character matched (exclusive index).
     * <p>
     * This is the position immediately following the last character of the matched subsequence.
     * This index is zero-based and is exclusive (one past the last matched character).
     * The substring from start() to end() represents the matched text.
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     *     FuzzyMatcher matcher = pattern.matcher(text);
     *     while(matcher.find()) {
     *         // Extract the matched text using start() and end()
     *         String matchedText = text.substring(matcher.start(), matcher.end());
     *         System.out.println("Found text: " + matchedText);
     *
     *         // This is equivalent to using foundText()
     *         System.out.println("Found text: " + matcher.foundText());
     *     }
     * }</pre>
     *
     * <p><strong>Edge Case:</strong> If the match is at the end of the input text,
     * end() will return the length of the input text.
     *
     * @return The offset after the last character matched (exclusive index).
     * @throws IllegalStateException if called when no match has been found.
     * @see #start()
     * @see #foundText()
     */
    int end();

    /**
     * Returns the matched subsequence from the input text.
     * <p>
     * This method provides direct access to the matched text without needing to use
     * {@link #start()} and {@link #end()} with the original text. For a matcher <code>m</code>
     * with input sequence <code>s</code>, the expressions <code>m.foundText()</code> and
     * <code>s.subSequence(m.start(), m.end())</code> are equivalent.
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     *     FuzzyMatcher matcher = pattern.matcher("The quick brown fox jumps over the lazy dog");
     *     while(matcher.find()) {
     *         CharSequence match = matcher.foundText();
     *         System.out.println("Match: " + match);
     *         System.out.println("Distance: " + matcher.distance());
     *     }
     * }</pre>
     *
     * <p><strong>Edge Cases:</strong>
     * <ul>
     *     <li>For exact matches, the returned text will be identical to the pattern (except for case if using case-insensitive matching)</li>
     *     <li>For fuzzy matches, the returned text will differ from the pattern by up to the maximum allowed Levenshtein distance</li>
     * </ul>
     *
     * @return The matched subsequence from the input text.
     * @throws IllegalStateException if called when no match has been found.
     * @see #start()
     * @see #end()
     * @see #distance()
     */
    CharSequence foundText();

    /**
     * Returns the similarity score between the matched text and the pattern.
     * <p>
     * The similarity is a float value between 0.0f and 1.0f, calculated as:
     * {@code (pattern.length() - distance()) / pattern.length()}
     * <p>
     * This provides a normalized measure of how closely the matched text resembles the pattern:
     * <ul>
     *     <li>1.0f indicates an exact match (distance = 0)</li>
     *     <li>Values closer to 0.0f indicate less similar matches</li>
     * </ul>
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     *     FuzzyMatcher matcher = pattern.matcher(text);
     *     while(matcher.find()) {
     *         System.out.println("Match: " + matcher.foundText());
     *         System.out.println("Similarity: " + matcher.similarity());
     *         // Filter only high-quality matches
     *         if (matcher.similarity() > 0.8f) {
     *             // Process high-quality matches
     *         }
     *     }
     * }</pre>
     *
     * <p><strong>Edge Cases:</strong>
     * <ul>
     *     <li>For exact matches, similarity will be 1.0f</li>
     *     <li>The minimum similarity depends on the maximum allowed Levenshtein distance and pattern length</li>
     *     <li>For a pattern of length n and maximum distance d, the minimum similarity will be (n-d)/n</li>
     * </ul>
     *
     * @return The similarity score between the matched text and the pattern, as a float between 0.0f and 1.0f.
     * @throws IllegalStateException if called when no match has been found.
     * @see #distance()
     * @see #pattern()
     */
    default float similarity() {
        return (pattern().text().length() - distance()) / (float) pattern().text().length();
    }

    /**
     * Returns the fuzzy pattern that was used to find this match.
     * <p>
     * This method provides access to the original pattern that was used to create the matcher
     * that found this match. This can be useful for accessing pattern properties such as:
     * <ul>
     *     <li>The pattern text via {@link FuzzyPattern#text()}</li>
     *     <li>The maximum allowed Levenshtein distance via {@link FuzzyPattern#maxLevenshteinDistance()}</li>
     *     <li>Whether the pattern is case-insensitive via {@link FuzzyPattern#caseInsensitive()}</li>
     * </ul>
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     *     FuzzyMatcher matcher = pattern.matcher(text);
     *     while(matcher.find()) {
     *         System.out.println("Pattern: " + matcher.pattern().text());
     *         System.out.println("Match: " + matcher.foundText());
     *         System.out.println("Max allowed distance: " + matcher.pattern().maxLevenshteinDistance());
     *         System.out.println("Actual distance: " + matcher.distance());
     *     }
     * }</pre>
     *
     * @return The reference to the fuzzy search pattern that was used to find this match.
     * @see FuzzyPattern
     * @see #distance()
     * @see #similarity()
     */
    FuzzyPattern pattern();

    /**
     * Returns the Levenshtein distance between the pattern text and the matched text.
     * <p>
     * The Levenshtein distance is the minimum number of single-character edits (insertions,
     * deletions, or substitutions) required to change the matched text into the pattern text.
     * This value will always be less than or equal to the maximum allowed Levenshtein distance
     * specified when creating the pattern.
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     *     // Create a pattern with maximum Levenshtein distance of 2
     *     FuzzyPattern pattern = FuzzyPattern.compile("example", 2);
     *     FuzzyMatcher matcher = pattern.matcher("This is an exmple text");
     *
     *     while(matcher.find()) {
     *         System.out.println("Match: " + matcher.foundText());
     *         System.out.println("Distance: " + matcher.distance());
     *
     *         // Get details about the edits
     *         matcher.streamEditOperations().forEach(op ->
     *             System.out.println("Edit: " + op.type() + " at position " + op.position())
     *         );
     *     }
     * }</pre>
     *
     * <p><strong>Edge Cases:</strong>
     * <ul>
     *     <li>For exact matches, distance will be 0</li>
     *     <li>The maximum possible distance is limited by the pattern's maxLevenshteinDistance</li>
     *     <li>Matches with higher distances represent less similar text</li>
     * </ul>
     *
     * @return The Levenshtein distance between the pattern text and the matched text.
     * @throws IllegalStateException if called when no match has been found.
     * @see #similarity()
     * @see #streamEditOperations()
     * @see #streamEditTypes()
     */
    int distance();

    /**
     * @deprecated Use {@link #streamEditOperations()} instead.
     */
    @Deprecated
    default Stream<Operation> streamEditsDetails() {
        return StreamSupport.stream(
                () -> new OperationsSpliterator(new OperationsIterator(this, false)),
                OperationsSpliterator.CHARACTERISTICS,
                false
        );
    }

    /**
     * Streams all operations for this match char-by-char, including matching operations.
     * <p>
     * This method returns a stream of {@link Operation} objects representing all operations
     * (insertions, deletions, replacements, and matches) that transform the matched text into
     * the pattern text. Unlike {@link #streamEditOperations()}, this stream includes matching
     * operations where characters are the same in both texts.
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     *     FuzzyPattern pattern = FuzzyPattern.compile("hello", 2);
     *     FuzzyMatcher matcher = pattern.matcher("helo world");
     *
     *     while(matcher.find()) {
     *         System.out.println("Match: " + matcher.foundText());
     *
     *         // Print all operations including matches
     *         matcher.streamCharByCharOperations().forEach(op -> {
     *             System.out.println("Operation: " + op.type() +
     *                               " at position " + op.position() +
     *                               " character: " + op.character());
     *         });
     *     }
     * }</pre>
     *
     * <p><strong>Edge Cases:</strong>
     * <ul>
     *     <li>For exact matches, all operations will be of type {@link OperationType#MATCHING}</li>
     *     <li>The stream will contain operations for every character in the pattern and matched text</li>
     * </ul>
     *
     * @return A stream of all operations (including matches) that transform the matched text into the pattern.
     * @throws IllegalStateException if called when no match has been found.
     * @see Operation
     * @see OperationType
     * @see #streamEditOperations()
     */
    default Stream<Operation> streamCharByCharOperations() {
        return StreamSupport.stream(
                () -> new OperationsSpliterator(new OperationsIterator(this, true)),
                OperationsSpliterator.CHARACTERISTICS,
                false
        );
    }

    /**
     * Streams detailed edit operations applied to transform the matched text into the pattern.
     * <p>
     * This method returns a stream of {@link Operation} objects representing the edit operations
     * (insertions, deletions, or replacements) that were applied to transform the matched text into
     * the pattern text. Unlike {@link #streamEditTypes()}, this provides detailed information about
     * each operation including the position and character involved. This stream does not include
     * matching operations (where characters are the same).
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     *     FuzzyPattern pattern = FuzzyPattern.compile("algorithm", 2);
     *     FuzzyMatcher matcher = pattern.matcher("This is an algorthm example");
     *
     *     while(matcher.find()) {
     *         System.out.println("Match: " + matcher.foundText() + " (distance: " + matcher.distance() + ")");
     *
     *         // Print detailed information about each edit
     *         matcher.streamEditOperations().forEach(op -> {
     *             System.out.println("Edit: " + op.type() +
     *                               " at position " + op.position() +
     *                               " character: " + op.character());
     *         });
     *     }
     * }</pre>
     *
     * <p><strong>Edge Cases:</strong>
     * <ul>
     *     <li>For exact matches (distance = 0), this stream will be empty</li>
     *     <li>The stream will contain exactly {@link #distance()} elements</li>
     * </ul>
     *
     * @return A stream of detailed edit operations applied to transform the matched text into the pattern.
     * @throws IllegalStateException if called when no match has been found.
     * @see Operation
     * @see OperationType
     * @see #distance()
     * @see #streamEditTypes()
     * @see #streamCharByCharOperations()
     */
    default Stream<Operation> streamEditOperations() {
        return StreamSupport.stream(
                () -> new OperationsSpliterator(new OperationsIterator(this, false)),
                OperationsSpliterator.CHARACTERISTICS,
                false
        );
    }

    /**
     * Streams the types of edit operations applied to the matched text to transform it into the pattern.
     * <p>
     * This method returns a stream of {@link OperationType} values representing the edit operations
     * (insertions, deletions, or replacements) that were applied to transform the matched text into
     * the pattern text. This stream does not include matching operations (where characters are the same).
     * The number of elements in this stream will be equal to the {@link #distance()}.
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     *     FuzzyPattern pattern = FuzzyPattern.compile("algorithm", 2);
     *     FuzzyMatcher matcher = pattern.matcher("This is an algorthm example");
     *
     *     while(matcher.find()) {
     *         System.out.println("Match: " + matcher.foundText());
     *         System.out.println("Distance: " + matcher.distance());
     *
     *         // Print the types of edits needed
     *         matcher.streamEditTypes().forEach(type ->
     *             System.out.println("Edit type: " + type)
     *         );
     *     }
     * }</pre>
     *
     * <p><strong>Edge Cases:</strong>
     * <ul>
     *     <li>For exact matches (distance = 0), this stream will be empty</li>
     *     <li>The stream will contain exactly {@link #distance()} elements</li>
     * </ul>
     *
     * @return A stream of edit operation types applied to transform the matched text into the pattern.
     * @throws IllegalStateException if called when no match has been found.
     * @see OperationType
     * @see #distance()
     * @see #streamEditOperations()
     * @see #streamCharByCharOperations()
     */
    Stream<OperationType> streamEditTypes();
}
