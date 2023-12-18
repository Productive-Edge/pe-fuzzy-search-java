package com.pe.text;

/**
 * Simplified implementation of the {@link java.util.BitSet}
 * Most of the operations mutates current instance
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
     * Sets one at the specified bit index on this instance and returns it.
     *
     * @param bitIndex specified bit index to set to zero.
     * @return this mutated instance
     */
    BitVector setOneAt(final int bitIndex) {
        words[bitIndex >>> 6] |= 1L << (bitIndex & 63);
        return this;
    }

    /**
     * Returns true if all bits set to zero, otherwise - false
     *
     * @return true if all bits set to zero, otherwise - false
     */
    boolean isZero() {
        for (long word : words)
            if (word != 0)
                return false;
        return true;
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
     * Sets all bits to 1 (one), which is analogue of -1 for signed integers
     *
     * @return this mutated instance where all bits are set to 1
     */
    BitVector resetToMinusOne() {
        for (int i = 0, l = words.length; i < l; i++) words[i] = -1L;
        return this;
    }

    /**
     * Sets all bits to 0 (zero), which is analogue of 0 for signed integers
     *
     * @return this mutated instance where all bits are set to 1
     */
    BitVector resetToZero() {
        for (int i = 0, l = words.length; i < l; i++) words[i] = 0;
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

//    /**
//     * Inverts bits of this vector
//     *
//     * @return this mutated instance with inverted bits
//     */
//    BitVector invert() {
//        for (int i = 0; i < words.length; i++) words[i] = ~words[i];
//        return this;
//    }
//

    /**
     * Shifts left all bits on one {@code << 1} in this instance
     *
     * @return this mutated instance with result of {@code << 1}
     */
    BitVector leftShift1() {
        long bit = 0L;
        for (int i = 0, l = words.length; i < l; i++) {
            final long wi = words[i];
            words[i] = (wi << 1) | bit;
            bit = wi >>> 63;
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
        long bits = 0L;
        final int reminder = 64 - (bitsCount & 63);
        for (int i = wordsCount, l = this.words.length; i < l; i++) {
            final long wi = this.words[i];
            this.words[i] = (wi << bitsCount) | bits;
            bits = wi >>> reminder;
        }
        return this;
    }
//
//    /**
//     * Returns true if this instance is not less than specified one, otherwise - false
//     * (i.e. {@code this >= vector}).
//     *
//     * @param vector to compare
//     * @return result of the {@code this >= vector})
//     */
//    boolean notLessThan(BitVector vector) {
//        return !lessThan(vector);
//    }
//
//
//    /**
//     * Returns true if this instance is less than specified one, otherwise - false
//     * (i.e. {@code this < vector}).
//     *
//     * @param vector to compare
//     * @return result of the {@code this < vector})
//     */
//    boolean lessThan(BitVector vector) {
//        for (int i = words.length - 1; i >= 0; i--) {
//            final long delta = words[i] - vector.words[i];
//            if (delta == 0L) continue;
//            return delta < 0L;
//        }
//        return false;
//    }
//
//    /**
//     * Performs bitwise XOR (^) operation on this and specified vector and stores result to this instance.
//     *
//     * @param vector 2nd vector
//     * @return this mutated instance with the result of the XOR bitwise operation
//     */
//    BitVector xor(BitVector vector) {
//        for (int i = 0; i < words.length; i++) words[i] ^= vector.words[i];
//        return this;
//    }
//
//    /**
//     * Returns true if the sign bit of the last 64-bit word is 0, otherwise - false
//     *
//     * @return true if the sign bit of the last 64-bit word is 0, otherwise - false
//     */
//    boolean isPositive() {
//        return !isNegative();
//    }
//
//    /**
//     * Returns true if the sign bit of the last 64-bit word is 0, otherwise - false
//     *
//     * @return true if the sign bit of the last 64-bit word is 0, otherwise - false
//     */
//    boolean isNegative() {
//        return words[words.length - 1] < 0L;
//    }
//
//    /**
//     * Returns this mutated instance where the highest 64-bit word was changed {@code  &= ~lastBitMask}.
//     * This method is used to determinate the maximum possible position
//     * for the matching of the character positions stored in this instance.
//     *
//     * @return this mutated instance where the highest 64-bit word was changed {@code  &= ~lastBitMask}
//     */
//    BitVector andInvertedLastBitMask() {
//        words[words.length - 1] &= ~lastBitMask;
//        return this;
//    }

    /**
     * Performs mutations as unsigned right shift (>>>) on one bit.
     *
     * @return the same instance, with modified bits
     */
    BitVector rightUnsignedShift1() {
        long bit = 0L;
        for (int i = words.length - 1; i >= 0; i--) {
            final long wi = words[i];
            words[i] = (words[i] >>> 1) | bit;
            bit = wi << 63;
        }
        return this;
    }
}
