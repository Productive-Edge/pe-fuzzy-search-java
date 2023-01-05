package by.gena.juzzy;

class BitVector {
    final long[] bits;
    final long lastBitMask;

    BitVector(final int length) {
        bits = new long[((length - 1) >>> 6) + 1];
        lastBitMask = 1L << ((length - 1) & 63L);
    }

    BitVector setZeroAt(final int bitIndex) {
        bits[bitIndex >>> 6] &= ~(1L << (bitIndex & 63));
        return this;
    }

    boolean hasZeroAtLastBit() {
        return 0L == (bits[bits.length - 1] & lastBitMask);
    }

    BitVector setMinusOne() {
        //        Arrays.fill(bits, value);
        for (int i = 0, l = bits.length; i < l; i++) bits[i] = -1L;
        return this;
    }

    BitVector setBitsFrom(final BitVector vector) {
        //        System.arraycopy(vector.bits, 0, bits, 0, bits.length); // is not effective! for short arrays
        for (int i = 0, l = bits.length; i < l; i++) bits[i] = vector.bits[i];
        return this;
    }

    BitVector or(final long firstBits) {
        bits[0] |= firstBits;
        return this;
    }

    BitVector or(final BitVector vector) {
        for (int i = 0, l = bits.length; i < l; i++) bits[i] |= vector.bits[i];
        return this;
    }

    BitVector and(final long firstBits) {
        bits[0] &= firstBits;
        return this;
    }

    BitVector and(final BitVector vector) {
        for (int i = 0, l = bits.length; i < l; i++) bits[i] &= vector.bits[i];
        return this;
    }

    BitVector invert() {
        for (int i = 0, l = bits.length; i < l; i++) bits[i] = ~bits[i];
        return this;
    }

    BitVector leftShift1() {
        long bit = 0;
        for (int i = 0, l = bits.length; i < l; i++) {
            final long overflow = bits[i] >>> 63;
            bits[i] <<= 1;
            bits[i] |= bit;
            bit = overflow;
        }
        return this;
    }

    BitVector leftShift(final int bitsCount) {
        final int words = bitsCount >>> 6;
        for(int i = 0; i < words; i++) bits[i] = 0L;
        long bit = 0;
        final int reminder = 64 - (bitsCount & 63);
        for (int i = words, l = bits.length; i < l; i++) {
            final long overflow = bits[i] >>> reminder;
            bits[i] <<= bitsCount;
            bits[i] |= bit;
            bit = overflow;
        }
        return this;
    }

    boolean lessThan(BitVector vector) {
        for (int i = bits.length - 1; i >= 0; i--) {
            final long delta = bits[i] - vector.bits[i];
            if (delta == 0L) continue;
            return  delta < 0L;
        }
        return false;
    }

    boolean isMinusOne() {
        for (final long bit : bits) {
            if (bit != -1L) return false;
        }
        return true;
    }
}
