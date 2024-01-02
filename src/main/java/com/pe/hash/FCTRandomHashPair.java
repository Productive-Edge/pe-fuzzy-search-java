package com.pe.hash;

import java.util.Arrays;
import java.util.Random;

/**
 * Perfect hash (not minimal) via random two hash functions with one level clashing allowed.
 * <p>
 * Creation / search of random functions is 3 times slower in average that fast utils map creation,
 * but search is ~2.5x faster ([1.5 - 3] times) than fast utils.
 * 2xN memory usage
 * </p>
 */
final class FCTRandomHashPair implements FixedCharTable {

    // tested 8 primes which show good distribution for usual text (low clashing rate)
    private static final int[] primes = new int[]{130531, 261619, 524789, 785857, 786901, 786949, 786959, 884483};

    private final int mod;
    private final int[] chars;
    private final int maxAttempts;
    int attempts;
    private int seed1;
    private int seed2;

    FCTRandomHashPair(int[] distinct, int maxAttempts) {
        this.maxAttempts = maxAttempts;
        this.attempts = 0;
        final int log2 = 1 + 32 - Integer.numberOfLeadingZeros(distinct.length);
        chars = new int[1 << log2];
        mod = (chars.length >> 1) - 1;

        Random r = new Random();
        boolean clashed = true;
        while (clashed && attempts-- < maxAttempts) {
            seed1 = r.nextInt();
            seed2 = r.nextInt();
            clashed = false;
            for (int i = 0; i < distinct.length; i++) {
                final int c = distinct[i];
                final int i1 = m1(c);
                if (chars[i1] < 0) {
                    chars[i1] = c;
                } else {
                    final int i2 = m2(c);
                    if (chars[i2] >= 0) {
                        clashed = true;
                        Arrays.fill(chars, -1);
                        break;
                    }
                    chars[i2] = c;
                }
            }
        }
    }

    private int m1(int x) {
        x = x * primes[seed2 & 7] + seed1;
        return ((x ^ (x >>> 16)) & mod) << 1;
    }

    private int m2(int x) {
        x = x * primes[seed1 & 7] + seed2;
        return (((x ^ (x >>> 16)) & mod) << 1) + 1;
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
