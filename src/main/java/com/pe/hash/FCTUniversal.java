package com.pe.hash;

import java.util.Arrays;

/**
 * An implementation of {@link FixedCharTable} using a simplified universal hashing approach.
 * <p>
 * This class provides a character lookup table using a hash table with linear probing for
 * collision resolution. It is used when the character set is too large for simpler implementations
 * like {@link FCT1} or {@link FCT2}, and when {@link FCTCuckoo} cannot find a perfect hash function.
 * <p>
 * The implementation uses a simple but effective hash function that distributes characters
 * evenly across the table to minimize collisions. Since the table is built once and then
 * used for lookups only (no deletions needed), the implementation is optimized for lookup
 * performance.
 * <p>
 * This class is not intended to be used directly. Instead, use the {@link FixedCharTable#from(CharSequence)}
 * factory method to get the most efficient implementation for a given character sequence.
 *
 * @see FixedCharTable
 * @see FCTCuckoo
 */
final class FCTUniversal implements FixedCharTable {

    final int mask;
    final int[] chars;
    int collisions = 0;

    /**
     * Constructs a new FCTUniversal instance for the given character sequence.
     * <p>
     * This constructor builds a hash table sized appropriately for the input character sequence.
     * The size is determined based on the length of the input to minimize collisions while
     * keeping memory usage reasonable. The table is then populated with all unique characters
     * from the input sequence.
     * <p>
     * The implementation uses linear probing to resolve collisions during table construction.
     * The number of collisions encountered is tracked for potential optimization decisions.
     *
     * @param charSequence The character sequence to build the lookup table for.
     */
    FCTUniversal(CharSequence charSequence) {
        final int b = log2(charSequence.length());
        final int maskSize = b + 1;
        chars = new int[1 << maskSize];
        Arrays.fill(chars, -1);
        mask = chars.length - 1;
        for (int i = 0; i < charSequence.length(); i++) {
            int ci = charSequence.charAt(i);
            int k = mix(ci);
            int p;
            int c;
            while ((c = chars[p = k & mask]) >= 0) {
                if (c == ci)
                    break;
                collisions++;
                k++;
            }
            chars[p] = ci;
        }
    }

    /**
     * Calculates the base-2 logarithm of an integer.
     * <p>
     * This utility method is used to determine the appropriate size for the hash table
     * based on the input character sequence length.
     *
     * @param x The integer to calculate the logarithm for.
     * @return The base-2 logarithm of x.
     */
    private static int log2(int x) {
        return 32 - Integer.numberOfLeadingZeros(x);
    }

    /**
     * Mixes a character code point to produce a hash value.
     * <p>
     * This method implements a simple but effective hash function that distributes
     * character values evenly across the hash table. It uses a combination of bit
     * manipulation and multiplication by a prime number to reduce clustering.
     *
     * @param charCodePoint The character code point to hash.
     * @return The hash value for the character.
     */
    int mix(int charCodePoint) {
        int h = charCodePoint >>> 8;
        return charCodePoint * FCTCuckoo.primes[(h ^ h >>> 4) & 7];
    }

    /**
     * Returns the index of the specified character in this table, or -1 if not found.
     * <p>
     * This method implements the {@link FixedCharTable#indexOf(char)} contract using
     * linear probing to handle collisions during lookup. It first hashes the character
     * using the {@link #mix(int)} method, then searches the table starting from that
     * position until it either finds the character or encounters an empty slot.
     *
     * @param c The character to look up in the table.
     * @return The index of the character in the table, or -1 if the character is not present.
     */
    @Override
    public int indexOf(char c) {
        int k = mix(c);
        int ci;
        int p;
        while ((ci = chars[p = k & mask]) >= 0) {
            if (ci == c) return p;
            k++;
        }
        return -1;
    }

    /**
     * Returns the size of this character table.
     * <p>
     * The size represents the capacity of the hash table, which is always a power of 2
     * to optimize the modulo operation using bitwise AND with a mask.
     *
     * @return The size of this character table.
     */
    @Override
    public int size() {
        return chars.length;
    }

    /**
     * Determines if this hash table has good properties for efficient lookups.
     * <p>
     * A "good" hash table has no collisions during construction and a low number of
     * adjacent filled slots. This method is used by the {@link FixedCharTable#from(CharSequence)}
     * factory method to decide whether to use this implementation or try another one.
     *
     * @return {@code true} if this hash table has good properties, {@code false} otherwise.
     */
    boolean isGood() {
        if (collisions > 0)
            return false;
        int neighbours = 0;
        for (int i = 1; i < chars.length; i++) {
            if ((chars[i] | chars[i - 1]) != -1)
                neighbours++;
        }
        return (neighbours << 4) < chars.length;
    }

}
