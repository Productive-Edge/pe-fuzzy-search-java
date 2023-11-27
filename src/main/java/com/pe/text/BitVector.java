package com.pe.text;

/**
 * Simplified implementation of the {@link java.util.BitSet}
 */
final class BitVector {
    final long[] words;
    final long lastBitMask;

    /**
     * Creates new instance of vector which consists of 64-bit words enough to store specified amount of bits
     *
     * @param length - specified amount of bits to store
     */
    BitVector(final int length) {
        words = new long[((length - 1) >>> 6) + 1];
        lastBitMask = 1L << ((length - 1) & 63);
    }

    /**
     * Sets zero at the specified bit index on this instance and returns it.
     *
     * @param bitIndex specified bit index to set to zero.
     * @return this mutated instance
     */
    BitVector setZeroAt(final int bitIndex) {
        words[bitIndex >>> 6] &= ~(1L << (bitIndex & 63));
        return this;
    }

    /**
     * Returns true if the last bit (specified at constructor) has zero, otherwise - false
     *
     * @return true if the last bit (specified at constructor) has zero, otherwise - false
     */
    boolean hasZeroAtTheLastBit() {
        return 0L == (words[words.length - 1] & lastBitMask);
    }


    /**
     * Sets all bits to 1 (one), which is analouge of -1 for signed integers
     *
     * @return this mutated instance where all bits are set to 1
     */
    BitVector setMinusOne() {
        for (int i = 0, l = words.length; i < l; i++) words[i] = -1L;
        return this;
    }


    /**
     * Copy bits from the specified bit vector into this instance
     *
     * @param vector source of copy operation
     * @return this mutated instance with bits set (copied) from the specified vector
     */
    BitVector setBitsFrom(final BitVector vector) {
        for (int i = 0, l = words.length; i < l; i++) words[i] = vector.words[i];
        return this;
    }

    /**
     * Performs bitwise OR (|) operation on this and specified vector and stores result to this instance.
     *
     * @param vector 2nd vector
     * @return this mutated instance with the result of the OR bitwise operation
     */
    BitVector or(final BitVector vector) {
        for (int i = 0; i < words.length; i++) words[i] |= vector.words[i];
        return this;
    }

    /**
     * Performs bitwise AND (&) operation on this and specified vector and stores result to this instance.
     *
     * @param vector 2nd vector
     * @return this mutated instance with the result of the AND bitwise operation
     */
    BitVector and(final BitVector vector) {
        for (int i = 0; i < words.length; i++) words[i] &= vector.words[i];
        return this;
    }

    /**
     * Inverts bits of this vector
     *
     * @return this mutated instance with inverted bits
     */
    BitVector invert() {
        for (int i = 0; i < words.length; i++) words[i] = ~words[i];
        return this;
    }

    /**
     * Shifts left all bits on one {@code << 1} in this instance
     *
     * @return this mutated instance with result of {@code << 1}
     */
    BitVector leftShift1() {
        long bit = 0;
        for (int i = 0, l = words.length; i < l; i++) {
            final long overflow = words[i] >>> 63;
            words[i] <<= 1;
            words[i] |= bit;
            bit = overflow;
        }
        return this;
    }

    /**
     * Shifts left all bits on the specified amount of bits {@code << bitsCount} in this instance
     *
     * @param bitsCount - specified amount of bits
     * @return this mutated instance with result of {@code << bitsCount}
     */
    BitVector leftShift(final int bitsCount) {
        final int wordsCount = bitsCount >>> 6;
        for (int i = 0; i < wordsCount; i++) this.words[i] = 0L;
        long bit = 0;
        final int reminder = 64 - (bitsCount & 63);
        for (int i = wordsCount, l = this.words.length; i < l; i++) {
            final long overflow = this.words[i] >>> reminder;
            this.words[i] <<= bitsCount;
            this.words[i] |= bit;
            bit = overflow;
        }
        return this;
    }

    /**
     * Returns true if this instance is not less than specified one, otherwise - false
     * (i.e. {@code this >= vector}).
     *
     * @param vector to compare
     * @return result of the {@code this >= vector})
     */
    boolean notLessThan(BitVector vector) {
        return !lessThan(vector);
    }


    /**
     * Returns true if this instance is less than specified one, otherwise - false
     * (i.e. {@code this < vector}).
     *
     * @param vector to compare
     * @return result of the {@code this < vector})
     */
    boolean lessThan(BitVector vector) {
        for (int i = words.length - 1; i >= 0; i--) {
            final long delta = words[i] - vector.words[i];
            if (delta == 0L) continue;
            return delta < 0L;
        }
        return false;
    }

    /**
     * Performs bitwise XOR (^) operation on this and specified vector and stores result to this instance.
     *
     * @param vector 2nd vector
     * @return this mutated instance with the result of the XOR bitwise operation
     */
    BitVector xor(BitVector vector) {
        for (int i = 0; i < words.length; i++) words[i] ^= vector.words[i];
        return this;
    }

    /**
     * Returns true if the sign bit of the last 64-bit word is 0, otherwise - false
     *
     * @return true if the sign bit of the last 64-bit word is 0, otherwise - false
     */
    boolean isPositive() {
        return !isNegative();
    }

    /**
     * Returns true if the sign bit of the last 64-bit word is 0, otherwise - false
     *
     * @return true if the sign bit of the last 64-bit word is 0, otherwise - false
     */
    boolean isNegative() {
        return words[words.length - 1] < 0L;
    }

    /**
     * Returns this mutated instance where the highest 64-bit word was changed {@code  &= ~lastBitMask}.
     * This method is used to determinate the maximum possible position
     * for the matching of the character positions stored in this instance.
     *
     * @return this mutated instance where the highest 64-bit word was changed {@code  &= ~lastBitMask}
     */
    BitVector andInvertedLastBitMask() {
        words[words.length - 1] &= ~lastBitMask;
        return this;
    }
}
