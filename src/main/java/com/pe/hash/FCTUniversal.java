package com.pe.hash;

import java.util.Arrays;

final class FCTUniversal implements FixedCharTable {

    final int mask;
    final int[] chars;

    FCTUniversal(CharSequence charSequence) {
        final int maskSize = 32 - Integer.numberOfLeadingZeros(charSequence.length() + 1);
        chars = new int[1 << maskSize];
        Arrays.fill(chars, -1);
        mask = chars.length - 1;
        charSequence.chars().forEach(ci -> {
            int k = mix(ci);
            int p, c;
            while ((c = chars[p = k & mask]) >= 0) {
                if (c == ci)
                    break;
                k++;
            }
            chars[p] = ci;
        });
    }

    static int mix(int charCodePoint) {
        final int p = charCodePoint * 0xF3853C3F;
        return p ^ (p >> 16);
    }

    @Override
    public int indexOf(char c) {
        int k = mix(c);
        int ci, p;
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
}
