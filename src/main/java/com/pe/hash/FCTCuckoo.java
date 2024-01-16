package com.pe.hash;

import java.util.Arrays;
import java.util.Random;

/**
 * Perfect hash (not minimal) via random two hash functions with one level clashing allowed.
 * This implementation is close to the Cuckoo hashing
 */
final class FCTCuckoo implements FixedCharTable {

    // tested 8 primes which show good distribution for usual text (low clashing rate)
    static final int[] primes = new int[]{130531, 261619, 524789, 785857, 786901, 786949, 786959, 884483};

    private final int mod;
    private final int[] chars;
    private final int maxAttempts;
    int attempts;
    private int seed1;
    private int seed2;

    FCTCuckoo(int[] distinct, int maxAttempts) {
        this.maxAttempts = maxAttempts;
        this.attempts = 0;
        final int log2 = 32 - Integer.numberOfLeadingZeros(distinct.length + (distinct.length >> 1));
        chars = new int[1 << log2];
        Arrays.fill(chars, -1);
        mod = (chars.length >> 1) - 1;

        Random r = new Random();
        boolean clashed = true;
        while (clashed && ++attempts < maxAttempts) {
            seed1 = seed(r.nextInt());
            seed2 = seed(r.nextInt());
            clashed = false;
            for (final int c : distinct) {
                final int i1 = m1(c);
                if (chars[i1] < 0) {
                    chars[i1] = c;
                } else {
                    final int i2 = m2(c);
                    if (chars[i2] < 0) {
                        chars[i2] = c;
                    } else {
                        final int i3 = m2(chars[i1]);
                        if (chars[i3] < 0) {
                            chars[i3] = chars[i1];
                            chars[i1] = c;
                        } else {
                            clashed = true;
                            Arrays.fill(chars, -1);
                            break;
                        }
                    }
                }
            }
        }
    }

    static int seed(int seed) {
        return primes[seed & 7] + seed;
    }

    private int m1(int x) {
        return m(x * seed1);
    }

    private int m2(int x) {
        return m(x * seed2) + 1;
    }

    private int m(int x) {
        return ((x ^ (x >>> 16)) & mod) << 1;
    }

    boolean found() {
        return attempts < maxAttempts;
    }

    @Override
    public int indexOf(char c) {
        final int i1 = m1(c);
        if (chars[i1] == c) return i1;
        final int i2 = m2(c);
        if (chars[i2] == c) return i2;
        return -1;
    }

    @Override
    public int size() {
        return chars.length;
    }
}
