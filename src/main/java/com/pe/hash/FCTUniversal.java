package com.pe.hash;

import java.util.Arrays;

/**
 * Simplified universal hashing (no need in the deletion)
 */
final class FCTUniversal implements FixedCharTable {

    final int mask;
    final int[] chars;
    int collisions = 0;

    FCTUniversal(CharSequence charSequence) {
        final int b = log2(charSequence.length());
        final int maskSize = b + 1;
        chars = new int[1 << maskSize];
        Arrays.fill(chars, -1);
        mask = chars.length - 1;
        charSequence.chars().forEach(ci -> {
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
        });
    }

    private static int log2(int x) {
        return 32 - Integer.numberOfLeadingZeros(x);
    }


    int mix(int charCodePoint) {
        int h = charCodePoint >>> 8;
        return charCodePoint * FCTCuckoo.primes[(h ^ h >>> 4) & 7];
    }

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

    @Override
    public int size() {
        return chars.length;
    }

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
