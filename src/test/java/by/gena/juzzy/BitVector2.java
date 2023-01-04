package by.gena.juzzy;

final class BitVector2 {
    final long[] bits;
    final long lastBitMask;

    BitVector2(final int length) {
        bits = new long[((length - 1) >>> 6) + 1];
        lastBitMask = 1L << ((length - 1) & 63L);
    }

    BitVector2 fill(final long value) {
//        Arrays.fill(bits, value); // is not effective for short array
        for (int i = 0, l = bits.length; i < l; i++) bits[i] = value;
        return this;
    }

    BitVector2 setZeroAt(final int bitIndex) {
        bits[bitIndex >>> 6] &= ~(1L << (bitIndex & 63));
        return this;
    }

    boolean hasZeroAtLastBit() {
        return 0L == (bits[bits.length - 1] & lastBitMask);
    }

    BitVector2 setBitsFrom(final BitVector2 vector) {
//        System.arraycopy(vector.bits, 0, bits, 0, bits.length); // not effective! for short arrays
        for (int i = 0, l = bits.length; i < l; i++) bits[i] = vector.bits[i];
        return this;
    }

    BitVector2 or(final BitVector2 vector) {
        for (int i = 0, l = bits.length; i < l; i++) {
            bits[i] |= vector.bits[i];
        }
        return this;
    }

    BitVector2 and(final BitVector2 vector) {
        for (int i = 0, l = bits.length; i < l; i++) {
            bits[i] &= vector.bits[i];
        }
        return this;
    }

    BitVector2 invert() {
        for (int i = 0, l = bits.length; i < l; i++) {
            bits[i] = ~bits[i];
        }
        return this;
    }

    BitVector2 leftShift1() {
        long bit = 0L;
        for (int i = 0, l = bits.length; i < l; i++) {
            final long overflow = bits[i] >>> 63;
            bits[i] <<= 1;
            bits[i] |= bit;
            bit = overflow;
        }
        return this;
    }

    boolean greaterOrEqualThan(final BitVector2 vector) {
        for (int i = bits.length - 1; i >= 0; i--) {
            final long delta = bits[i] - vector.bits[i];
            if (delta == 0L) continue;
            return delta > 0L;
        }
        return true;
    }

    boolean isMinusOne() {
        for (final long bit : bits) {
            if (bit != -1L) return false;
        }
        return true;
    }
}
