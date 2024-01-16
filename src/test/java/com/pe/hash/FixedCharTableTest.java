package com.pe.hash;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class FixedCharTableTest {

    @Test
    void testPerf() {
        FixedCharTable test = FixedCharTable.from("test of testing");
        assertTrue(test.indexOf('t') >= 0);
        assertTrue(test.indexOf('e') >= 0);
        assertTrue(test.indexOf('s') >= 0);
        assertTrue(test.indexOf(' ') >= 0);
        assertTrue(test.indexOf('o') >= 0);
        assertTrue(test.indexOf('f') >= 0);
        assertTrue(test.indexOf('i') >= 0);
        assertTrue(test.indexOf('n') >= 0);
        assertTrue(test.indexOf('g') >= 0);

        assertFalse(test.indexOf('z') >= 0);
        assertFalse(test.indexOf('b') >= 0);
        assertFalse(test.indexOf('k') >= 0);
    }

    @Disabled("not ready yet")
    @Test
    void testMinPerfHash_Ascii() {
        Random r = new Random();
        IntStream lengths = r.ints(3, 64);
        StringBuilder sb = new StringBuilder(64);
        lengths.limit(100000).mapToObj(l ->
                r.ints(' ', 'z').limit(l)
                        .collect(() -> {
                            sb.setLength(0);
                            return sb;
                        }, StringBuilder::appendCodePoint, StringBuilder::append)
        ).forEach(s -> {
            FCTMinPerfHash hash = FCTMinPerfHash.findFor(Arrays.stream(new FCTUniversal(s).chars).filter(c -> c >= 0).sorted().toArray());
            assertNotNull(hash, "No multiplier for " + s);
        });
    }

    @Disabled("not ready yet")
    @Test
    void testMinPerfHash_Cyr() {
        Random r = new Random();
        IntStream lengths = r.ints(3, 64);
        StringBuilder sb = new StringBuilder(64);
        lengths.limit(100000).mapToObj(l ->
                r.ints('А', 'я').limit(l)
                        .collect(() -> {
                            sb.setLength(0);
                            return sb;
                        }, StringBuilder::appendCodePoint, StringBuilder::append)
        ).forEach(s -> {
            FCTMinPerfHash hash = FCTMinPerfHash.findFor(Arrays.stream(new FCTUniversal(s).chars).filter(c -> c >= 0).sorted().toArray());
            assertNotNull(hash, "No multiplier for " + s);
        });
    }

    @Disabled("not ready yet")
    @Test
    void testMinPerfHash_Utf() {
        Random r = new Random();
        IntStream lengths = r.ints(3, 64);
        StringBuilder sb = new StringBuilder(64);
        lengths.limit(100000).mapToObj(l ->
                r.ints(' ', (char) 0xffff).limit(l)
                        .collect(() -> {
                            sb.setLength(0);
                            return sb;
                        }, StringBuilder::appendCodePoint, StringBuilder::append)
        ).forEach(s -> {
            FCTMinPerfHash hash = FCTMinPerfHash.findFor(Arrays.stream(new FCTUniversal(s).chars).filter(c -> c >= 0).sorted().toArray());
            assertNotNull(hash, "No multiplier for " + s);
        });
    }

    @Test
    void testUniversal_Ascii() {
        Random r = new Random();
        int[] counts = new int[1000];
        final int N = 100000;
        final int maxLen = 128;
        int[] sumByLen = new int[maxLen];
        int[] cntByLen = new int[maxLen];
        IntStream lengths = r.ints(1, maxLen);
        StringBuilder sb = new StringBuilder(maxLen);
        lengths.limit(N).mapToObj(l ->
                r.ints(' ', 'z').limit(l)
                        .collect(() -> {
                            sb.setLength(0);
                            return sb;
                        }, StringBuilder::appendCodePoint, StringBuilder::append)
        ).forEach(s -> {
            FCTUniversal universal = new FCTUniversal(s);
            assertTrue(universal.collisions < counts.length);
            counts[universal.collisions]++;
            sumByLen[s.length()] += universal.collisions;
            cntByLen[s.length()]++;
        });
        IntStream.range(0, counts.length).filter(i -> counts[i] > 0)
                .forEach(i -> System.out.println(counts[i] + " hashes with " + i + " collisions"));
        System.out.println("Length (count of hashes, count of collisions) - avg. amount of collisions");
        IntStream.range(1, sumByLen.length).filter(i -> sumByLen[i] > 0)
                .forEach(i -> System.out.println(i + " (" + cntByLen[i] + ", " + sumByLen[i] + ") " + (cntByLen[i] > 0 ? sumByLen[i] / (float) cntByLen[i] : 0f)));
    }

    @Test
    void testUniversal_Utf() {
        Random r = new Random();
        int[] counts = new int[1000];
        final int N = 100000;
        final int maxLen = 128;
        int[] sumByLen = new int[maxLen];
        int[] cntByLen = new int[maxLen];
        IntStream lengths = r.ints(1, maxLen);
        StringBuilder sb = new StringBuilder(maxLen);
        lengths.limit(N).mapToObj(l ->
                r.ints(' ', (char) 0xffff).limit(l)
                        .collect(() -> {
                            sb.setLength(0);
                            return sb;
                        }, StringBuilder::appendCodePoint, StringBuilder::append)
        ).forEach(s -> {
            FCTUniversal universal = new FCTUniversal(s);
            assertTrue(universal.collisions < counts.length);
            counts[universal.collisions]++;
            sumByLen[s.length()] += universal.collisions;
            cntByLen[s.length()]++;
        });
        IntStream.range(0, counts.length).filter(i -> counts[i] > 0)
                .forEach(i -> System.out.println(counts[i] + " hashes with " + i + " collisions"));
        System.out.println("Length (count of hashes, count of collisions) - avg. amount of collisions");
        IntStream.range(1, sumByLen.length).filter(i -> sumByLen[i] > 0)
                .forEach(i -> System.out.println(i + " (" + cntByLen[i] + ", " + sumByLen[i] + ") " + (cntByLen[i] > 0 ? sumByLen[i] / (float) cntByLen[i] : 0f)));
    }

    @Test
    void testUniversal_Cyr() {
        Random r = new Random();
        int[] counts = new int[1000];
        final int N = 100000;
        final int maxLen = 128;
        int[] sumByLen = new int[maxLen];
        int[] cntByLen = new int[maxLen];
        IntStream lengths = r.ints(1, maxLen);
        StringBuilder sb = new StringBuilder(maxLen);
        lengths.limit(N).mapToObj(l ->
                r.ints('А', 'я').limit(l)
                        .collect(() -> {
                            sb.setLength(0);
                            return sb;
                        }, StringBuilder::appendCodePoint, StringBuilder::append)
        ).forEach(s -> {
            FCTUniversal universal = new FCTUniversal(s);
            assertTrue(universal.collisions < counts.length);
            counts[universal.collisions]++;
            sumByLen[s.length()] += universal.collisions;
            cntByLen[s.length()]++;
        });
        IntStream.range(0, counts.length).filter(i -> counts[i] > 0)
                .forEach(i -> System.out.println(counts[i] + " hashes with " + i + " collisions"));
        System.out.println("Length (count of hashes, count of collisions) - avg. amount of collisions");
        IntStream.range(1, sumByLen.length).filter(i -> sumByLen[i] > 0)
                .forEach(i -> System.out.println(i + " (" + cntByLen[i] + ", " + sumByLen[i] + ") " + (cntByLen[i] > 0 ? sumByLen[i] / (float) cntByLen[i] : 0f)));
    }


    @Test
    void testCuckoo_Ascii() {
        Random r = new Random();
        final int maxAttempts = 100;
        int[] counts = new int[maxAttempts + 1];
        final int N = 100000;
        final int maxLen = 64;
        int[] sumByLen = new int[maxLen];
        int[] cntByLen = new int[maxLen];
        IntStream lengths = r.ints(1, maxLen);
        StringBuilder sb = new StringBuilder(maxLen);
        lengths.limit(N).mapToObj(l ->
                r.ints(' ', 'z').limit(l)
                        .collect(() -> {
                            sb.setLength(0);
                            return sb;
                        }, StringBuilder::appendCodePoint, StringBuilder::append)
        ).forEach(s -> {
            FCTUniversal universal = new FCTUniversal(s);
            FCTCuckoo hashPair = new FCTCuckoo(Arrays.stream(universal.chars).filter(c -> c >= 0).toArray(), maxAttempts);
            assertTrue(hashPair.found(), "hashPair.found()");
            assertTrue(hashPair.attempts < counts.length);
            counts[hashPair.attempts]++;
            sumByLen[s.length()] += hashPair.attempts;
            cntByLen[s.length()]++;
        });

        IntStream.range(0, counts.length).filter(i -> counts[i] > 0).forEach(i ->
                System.out.println("Generated from attempt: " + i + ", count " + counts[i]));

        System.out.println("Length (count, attempts) avg. attempts");
        for (int i = 1; i < sumByLen.length; i++) {
            System.out.println(i + " (" + cntByLen[i] + ", " + sumByLen[i] + ") " + (cntByLen[i] > 0 ? sumByLen[i] / (float) cntByLen[i] : 0f));
        }
    }

    @Test
    void testCuckoo_Cyr() {
        Random r = new Random();
        final int maxAttempts = 100;
        int[] counts = new int[maxAttempts + 1];
        final int N = 100000;
        final int maxLen = 64;
        int[] sumByLen = new int[maxLen];
        int[] cntByLen = new int[maxLen];
        IntStream lengths = r.ints(1, maxLen);
        StringBuilder sb = new StringBuilder(maxLen);
        lengths.limit(N).mapToObj(l ->
                r.ints('А', 'я').limit(l)
                        .collect(() -> {
                            sb.setLength(0);
                            return sb;
                        }, StringBuilder::appendCodePoint, StringBuilder::append)
        ).forEach(s -> {
            FCTUniversal universal = new FCTUniversal(s);
            FCTCuckoo hashPair = new FCTCuckoo(Arrays.stream(universal.chars).filter(c -> c >= 0).toArray(), maxAttempts);
            assertTrue(hashPair.found(), "hashPair.found()");
            assertTrue(hashPair.attempts < counts.length);
            counts[hashPair.attempts]++;
            sumByLen[s.length()] += hashPair.attempts;
            cntByLen[s.length()]++;
        });

        IntStream.range(0, counts.length).filter(i -> counts[i] > 0).forEach(i ->
                System.out.println("Generated from attempt: " + i + ", count " + counts[i]));

        System.out.println("Length (count, attempts) avg. attempts");
        for (int i = 1; i < sumByLen.length; i++) {
            System.out.println(i + " (" + cntByLen[i] + ", " + sumByLen[i] + ") " + (cntByLen[i] > 0 ? sumByLen[i] / (float) cntByLen[i] : 0f));
        }
    }

    @Test
    void testCuckoo_Utf() {
        Random r = new Random();
        final int maxAttempts = 100;
        int[] counts = new int[maxAttempts + 1];
        final int N = 100000;
        final int maxLen = 64;
        int[] sumByLen = new int[maxLen];
        int[] cntByLen = new int[maxLen];
        IntStream lengths = r.ints(1, maxLen);
        StringBuilder sb = new StringBuilder(maxLen);
        lengths.limit(N).mapToObj(l ->
                r.ints(' ', (char) 0xffff).limit(l)
                        .collect(() -> {
                            sb.setLength(0);
                            return sb;
                        }, StringBuilder::appendCodePoint, StringBuilder::append)
        ).forEach(s -> {
            FCTUniversal universal = new FCTUniversal(s);
            FCTCuckoo hashPair = new FCTCuckoo(Arrays.stream(universal.chars).filter(c -> c >= 0).toArray(), maxAttempts);
            assertTrue(hashPair.found(), "hashPair.found()");
            assertTrue(hashPair.attempts < counts.length);
            counts[hashPair.attempts]++;
            sumByLen[s.length()] += hashPair.attempts;
            cntByLen[s.length()]++;
        });

        IntStream.range(0, counts.length).filter(i -> counts[i] > 0).forEach(i ->
                System.out.println("Generated from attempt: " + i + ", count " + counts[i]));

        System.out.println("Length (count, attempts) avg. attempts");
        for (int i = 1; i < sumByLen.length; i++) {
            System.out.println(i + " (" + cntByLen[i] + ", " + sumByLen[i] + ") " + (cntByLen[i] > 0 ? sumByLen[i] / (float) cntByLen[i] : 0f));
        }
    }

    @Test
    void testFinal() {
        Random r = new Random();
        int[] stats = new int[3];
        final int N = 100000;
        final int maxLen = 64;
        IntStream lengths = r.ints(3, maxLen);
        StringBuilder sb = new StringBuilder(maxLen);
        lengths.limit(N).mapToObj(l ->
                r.ints(' ', 'z').limit(l)
                        .collect(() -> {
                            sb.setLength(0);
                            return sb;
                        }, StringBuilder::appendCodePoint, StringBuilder::append)
        ).forEach(s -> {
            FixedCharTable fct = FixedCharTable.from(s);
            if (fct instanceof FCTUniversal) {
                FCTUniversal u = (FCTUniversal) fct;
                if (u.isGood()) {
                    stats[0]++;
                } else {
                    stats[2]++;
                }
            } else {
                stats[1]++;
            }
        });
        System.out.println("Universal " + (stats[0] * 100f / N));
        System.out.println("Cuckoo " + (stats[1] * 100f / N));
        System.out.println("Fallback " + (stats[2] * 100f / N));
        assertEquals(0, stats[2], "fallback");
    }


//    @Test
//    void textMix() {
//        boolean[] composite = new boolean[1000000];
////        for (int i = 2; i < 1000; i++) {
////            for (int j = i * 2; j < composite.length; j += i) {
////                composite[j] = true;
////            }
////        }
//
//        int[] candidates = new int[]{
//                130531,
//                261619,
//                524789,
//                785857,
//                786901,
//                786949,
//                786959,
//                884483,
//        };
//
//        Random r = new Random();
//        for (int i = 0; i < candidates.length; i++) {
//            FixedCharTable.FCTCuckoo._p = candidates[i];
//            IntStream lengths = r.ints(5, 64);
//            StringBuilder sb = new StringBuilder(64);
//            int sum = lengths.limit(100000).mapToObj(l ->
//                            r.ints(' ', 'я').limit(l)
//                                    .collect(() -> {
//                                        sb.setLength(0);
//                                        return sb;
//                                    }, StringBuilder::appendCodePoint, StringBuilder::append)
//                    ).map(FixedCharTable::from)
//                    .mapToInt(in -> ((FixedCharTable.FCTCuckoo) in).clashCount)
//                    .sum();
//            System.out.println(candidates[i] + "\t" + sum);
//        }
//    }

}