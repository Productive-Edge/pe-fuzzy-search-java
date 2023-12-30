package com.pe.hash;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharHashTest {

    @Test
    void testBitMask() {
        String s = "Sets ";
        int[] chars = s.chars().sorted().distinct().toArray();
        SeedFinder finder = new SeedFinder(chars);
        assertTrue(finder.found);
        for (int i = 0; i < chars.length; i++) {
            int hash = chars[i] * finder.seed;
            hash ^= (hash >>> 16);
            assertEquals(i, hash & finder.mask);
        }
    }

    static class SeedFinder {

        final List<CharProduct> chars;
        final int maskSize;
        final int mask;
        final long minMaxMask;
        final int prefixMask;
        final int prefixStep;
        final boolean found;

        int seed;

        SeedFinder(int[] chars) {
            if (chars == null || chars.length == 0) {
                throw new IllegalArgumentException("No character code points");
            }
            if (chars[0] <= 0) {
                throw new IllegalArgumentException("Negative or zero character code points");
            }

            maskSize = 32 - Integer.numberOfLeadingZeros(chars.length - 1);
            mask = (1 << maskSize) - 1;
            minMaxMask = 0x0000FFFF & ~mask;
            prefixStep = 1 << (16 + maskSize);
            prefixMask = -prefixStep;
            if (chars.length == 1) {
                found = true;
                this.chars = Collections.emptyList();
                return; // 0 is valid seed for this single value :)
            }

            this.chars = Arrays.stream(chars).distinct().mapToObj(CharProduct::new).collect(Collectors.toList());
            if (this.chars.size() != chars.length)
                throw new IllegalArgumentException("Duplicated character code points are not acceptable");


            PriorityQueue<CharProduct> minMax = new PriorityQueue<>(chars.length, Comparator.comparingLong(CharProduct::getMaxX));
            for (int lowBits = mask; lowBits >= 0; lowBits--) {
                seed = lowBits;
                long maxMin = -1L;
                for (int i = 0; i < chars.length; i++) {
                    CharProduct cpi = this.chars.get(i);
                    cpi.init(i);
                    maxMin = Math.max(maxMin, cpi.getMinX());
                    minMax.add(cpi);
                }
                while (true) {
                    CharProduct min = minMax.remove();
                    if (min.getMaxX() >= maxMin) {
                        if (min.tuneMaxX() >= maxMin) {
                            do {
                                final long oldMaxMin = maxMin;
                                maxMin = this.chars.stream().mapToLong(cp -> cp.tuneMinX(oldMaxMin)).filter(mm -> mm > oldMaxMin)
                                        .findFirst().orElse(oldMaxMin);
                                if (min.getMaxX() < maxMin)
                                    break;
                                if (maxMin == oldMaxMin) {
                                    seed = (int) maxMin;
                                    found = true;
                                    return;
                                }
                            } while (true);
                        }
                    }

                    if (!min.next(maxMin)) {
                        minMax.clear();
                        break;
                    }
                    maxMin = Math.max(maxMin, min.getMinX());
                    minMax.add(min);
                }
            }
            found = false;
        }


        public int getSeed() {
            return seed;
        }

        class CharProduct {
            private final int c;
            long minBits;
            long minX;
            long maxBits;
            long maxX;

            private int mod;

            CharProduct(int c) {
                this.c = c;
            }

            void init(int mod) {
                this.mod = mod;
                long bits = (c * (long) seed) & ~minMaxMask;
                bits |= ((bits ^ mod) << 16);
                maxBits = bits | minMaxMask;
                minBits = bits & ~minMaxMask;
                maxX = maxBits / c;
                minX = minBits / c;
            }

            boolean next(long maxMin) {
                long prefix = (c * maxMin) & prefixMask;
                do {
                    maxBits = prefix | (maxBits & ~prefixMask);
                    maxX = maxBits / c;
                    if (maxX >= maxMin)
                        break;
                    prefix += prefixStep;
                } while (true);
                minBits = prefix | (minBits & ~prefixMask);
                minX = minBits / c;
                return minX < 0x0FFFFFFFFL;
            }


            final long getMaxX() {
                return maxX;
            }

            final long getMinX() {
                return minX;
            }

            public long tuneMaxX() {
                long p = c * maxX;
                while (((p ^ (p >>> 16)) & mask) != mod) {
                    maxX--;
                    p -= c;
                }
                return maxX;
            }

            long tuneMinX(long from) {
                long p = c * from;
                while (((p ^ (p >>> 16)) & mask) != mod) {
                    from++;
                    p += c;

                }
                return minX = from;
            }
        }
    }
}