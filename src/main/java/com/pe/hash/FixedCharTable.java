package com.pe.hash;

/**
 * An interface for efficient character lookup tables used in fuzzy search algorithms.
 * <p>
 * FixedCharTable provides a way to quickly check if a character is present in a predefined set
 * and retrieve its position in the table. This is used internally by the Bitap algorithm
 * implementations to optimize pattern matching operations.
 * <p>
 * The implementation automatically selects the most efficient table implementation based on
 * the number of distinct characters:
 * <ul>
 *     <li>For empty sets: {@link FCT0}</li>
 *     <li>For single character: {@link FCT1}</li>
 *     <li>For two characters: {@link FCT2}</li>
 *     <li>For larger sets: {@link FCTCuckoo} or {@link FCTUniversal}</li>
 * </ul>
 *
 * @see FCTUniversal
 * @see FCTCuckoo
 * @see FCT0
 * @see FCT1
 * @see FCT2
 */
interface FixedCharTable {

    /**
     * Creates the most efficient FixedCharTable implementation for the given character sequence.
     * <p>
     * This factory method analyzes the input character sequence and selects the optimal
     * implementation based on the number of distinct characters:
     * <ul>
     *     <li>For null or empty sequences: Returns a singleton {@link FCT0} instance</li>
     *     <li>For a single unique character: Returns an {@link FCT1} instance</li>
     *     <li>For exactly two unique characters: Returns an {@link FCT2} instance</li>
     *     <li>For more characters: Attempts to create an {@link FCTCuckoo} instance, falling back to
     *         {@link FCTUniversal} if a perfect hash function cannot be found</li>
     * </ul>
     *
     * <p>This method optimizes for both memory usage and lookup performance.
     *
     * @param charSequence The character sequence to create a lookup table for.
     * @return The most efficient FixedCharTable implementation for the given character sequence.
     */
    static FixedCharTable from(CharSequence charSequence) {
        if (charSequence == null || charSequence.length() == 0) return FCT0.INSTANCE;
        FCTUniversal universal = new FCTUniversal(charSequence);
        int[] chars = universal.chars;
        int count = 0;
        for (int c : chars) {
            if (c >= 0) count++;
        }
        int[] distinct = new int[count];
        int i = 0;
        for (int c : chars) {
            if (c >= 0) distinct[i++] = c;
        }
        if (distinct.length == 1) return new FCT1(distinct[0]);
        if (distinct.length == 2) return new FCT2(distinct[0], distinct[1]);
        FCTCuckoo randomHashPair = new FCTCuckoo(distinct, 100);
        if (randomHashPair.found())
            return randomHashPair;
        return universal;
    }

    /**
     * Returns the index of the specified character in this table, or -1 if not found.
     * <p>
     * This method is used to quickly determine if a character is present in the set
     * and to get its position in the table. The exact meaning of the returned index
     * depends on the specific implementation, but it will always be non-negative if
     * the character is present.
     *
     * @param c The character to look up at the table.
     * @return The index of the character in the table, or -1 if the character is not present.
     */
    int indexOf(char c);

    /**
     * Returns the size of this character table.
     * <p>
     * The size represents the capacity of the table, not necessarily the number of
     * distinct characters it contains. This is used internally by the Bitap algorithm
     * implementations for memory allocation and bit manipulation operations.
     *
     * @return The size of this character table.
     */
    int size();


}
