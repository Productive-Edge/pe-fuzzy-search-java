package com.pe.hash;

import java.util.Arrays;

/**
 * Finding of the single multiplier for hash function is quite slow, but the fastest indexOf
 * (a bit faster than {@link FCTCuckoo})
 * 4xN memory inside :(
 */
final class FCTMinPerfHash implements FixedCharTable {
    private final int[] chars;
    private final int[] mods;
    private final int mask;
    private final int shift;

    int multiplier;

    private FCTMinPerfHash(final int[] chars) {
        this.chars = chars;
        final int maskSize = 2 + 32 - Integer.numberOfLeadingZeros(chars.length - 1);
        this.shift = 32 - maskSize; // maskSize << 1;
        this.mods = new int[1 << maskSize];
        this.mask = mods.length - 1;
        Arrays.fill(mods, -1);
    }

    public static FCTMinPerfHash findFor(int[] chars) {
        if (chars == null || chars.length == 0) {
            throw new IllegalArgumentException("No character code points");
        }

        if (chars[0] <= 0) {
            throw new IllegalArgumentException("Negative or zero character code points");
        }

        if (chars.length == 1) return new FCTMinPerfHash(chars);

        if (Integer.MAX_VALUE == Arrays.stream(chars).reduce(-1, (min, x) -> min < x ? x : Integer.MAX_VALUE)) {
            throw new IllegalArgumentException("Code points must be sorted and distinct");
        }

        FCTMinPerfHash hash = new FCTMinPerfHash(chars);
        Finder finder = hash.new Finder();
        for (hash.multiplier = hash.mask; hash.multiplier >= 0; hash.multiplier--) {
            if (finder.find()) return hash;
        }
        return null;
    }

    @Override
    public int indexOf(char c) {
        return indexOf((int) c);
    }

    int indexOf(int charCodePoint) {
        int index = mods[mod(charCodePoint * multiplier)];
        if (index >= 0 && chars[index] == charCodePoint) return index;
        return -1;
    }

    int mod(final int product) {
        return (product ^ (product >>> shift)) & mask;
    }

    @Override
    public int size() {
        return chars.length;
    }

    final class Finder {
        final int rangeMask = ((1 << shift) - 1) & ~mask;

        int align(int x) {
            return (x & ~mask) | multiplier;
        }

        boolean find() {
            final int i = chars.length - 1;
            final int ci = chars[i];
            final int lowBits = (ci * multiplier) & mask;
            for (int p = mask; p >= 0; p--) {
                final int prefixed = lowBits | (p << shift);
                final int maxNA = Integer.divideUnsigned(prefixed | rangeMask, ci);
                int max = align(maxNA);
                if (max > maxNA) max -= mods.length; // 1 << maskSize
                if (((max * ci) >>> shift) != p)
                    continue;
                final int minNA = Integer.divideUnsigned(prefixed/* & ~rangeMask*/, ci);
                int min = align(minNA);
                if (min < minNA) min += mods.length;
                if (((min * ci) >>> shift) != p)
                    continue;

                if (max >= min) {
                    final int mod = (p ^ lowBits) & mask;
                    mods[mod] = i;
                    if (i == 0) {
                        multiplier = min;
                        return true;
                    }
                    if (findFrom(i - 1, min, max))
                        return true;
                    mods[mod] = -1;
                }
            }
            return false;
        }

        boolean findFrom(int i, int maxMin, int minMax) {
            final int ci = chars[i];
            final int maxP = (minMax * ci) >>> shift;
            final int minP = (maxMin * ci) >>> shift;
            final int lowBits = (ci * multiplier) & mask;
            for (int p = maxP; p >= minP; p--) {
                final int mod = (p ^ lowBits) & mask;
                if (mods[mod] >= 0)
                    continue;
                final int prefixed = lowBits | (p << shift);
                final int maxNA = Integer.divideUnsigned(prefixed | rangeMask, ci);
                int max = align(maxNA);
                if (max > maxNA) max -= mods.length; // 1 << maskSize
                if (((max * ci) >>> shift) != p)
                    continue;
                final int minNA = Integer.divideUnsigned(prefixed/* & ~rangeMask*/, ci);
                int min = align(minNA);
                if (min < minNA) min += mods.length;
                if (((min * ci) >>> shift) != p)
                    continue;
                if (min < maxMin) min = maxMin;
                if (max > minMax) max = minMax;
                if (max >= min) {
                    mods[mod] = i;
                    if (i == 0) {
                        multiplier = min;
                        return true;
                    }
                    if (findFrom(i - 1, min, max)) return true;
                    mods[mod] = -1;
                }
            }
            return false;
        }
    }
}
