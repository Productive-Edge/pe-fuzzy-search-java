package com.pe.hash;

import java.util.Arrays;
import java.util.Random;

interface FixedCharTable {

    static FixedCharTable from(CharSequence charSequence) {
        if (charSequence == null || charSequence.length() == 0) return C0.INSTANCE;
        int[] distinct = charSequence.chars().sorted().distinct().toArray();
        if (distinct.length == 1) return new C1(distinct[0]);
        if (distinct.length == 2) return new C2(distinct[0], distinct[1]);
        return new PerfHash(distinct);
    }

    int indexOf(char c);

    int size();

    final class C0 implements FixedCharTable {
        static final C0 INSTANCE = new C0();

        @Override
        public int indexOf(char c) {
            return -1;
        }

        @Override
        public int size() {
            return 0;
        }
    }

    final class C1 implements FixedCharTable {
        private final int single;

        C1(int single) {
            this.single = single;
        }

        @Override
        public int indexOf(char c) {
            return single == c ? 0 : -1;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    final class C2 implements FixedCharTable {

        private final int first;
        private final int second;

        C2(final int first, final int second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public int indexOf(char c) {
            if (c == first) return 0;
            return c == second ? 1 : -1;
        }

        @Override
        public int size() {
            return 2;
        }
    }

    /**
     * perfect hash (4xN memory usage), but about twice (3-1.5 times) faster than fast utils
     */
    final class PerfHash implements FixedCharTable {

        // tested 8 primes, which show good distribution for characters (low clashing rate)
        private static final int[] primes = new int[]{130531, 261619, 524789, 785857, 786901, 786949, 786959, 884483};


        private final int mod;
        private final int[] t1;
        private final int[] t2;
        //        int clashCount = 0;
        private int seed1;
        private int seed2;

        PerfHash(int[] distinct) {
            final int log2 = 32 - Integer.numberOfLeadingZeros(distinct.length << 1);
            t1 = new int[1 << log2];
            t2 = new int[t1.length];
            mod = t1.length - 1;
            Random r = new Random();
            boolean clashed = true;
            while (clashed /*&& clashCount < 100*/) {
                seed1 = r.nextInt();
                seed2 = r.nextInt();
                clashed = false;
                for (final int c : distinct) {
                    final int i1 = m1(c);
                    if (t1[i1] == 0) {
                        t1[i1] = c;
                    } else {
                        final int i2 = m2(c);
                        if (t2[i2] != 0) {
                            clashed = true;
                            Arrays.fill(t1, 0);
                            Arrays.fill(t2, 0);
//                            clashCount++;
                            break;
                        }
                        t2[i2] = c;
                    }
                }
            }
//            if (clashCount > 5) {
//                System.out.println(clashCount);
//            }
        }

        int m1(int x) {
            x = x * primes[seed2 & 7] + seed1;
            return (x ^ (x >>> 16)) & mod;
        }

        int m2(int x) {
            x = x * primes[seed1 & 7] + seed2;
            return (x ^ (x >>> 16)) & mod;
        }

        @Override
        public int indexOf(char c) {
            final int i1 = m1(c);
            if (t1[i1] == c) return i1;
            final int i2 = m2(c);
            if (t2[i2] == c) return i2 + t1.length;
            return -1;
        }

        @Override
        public int size() {
            return t1.length << 1;
        }
    }

}
