package com.pe.hash;

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

    @Test
    void testRandomR2() {
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
            assertNotNull(hash, s::toString);
        });
    }

    @Test
    void testRandomM() {
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
            FCTRandomHashPair hash = new FCTRandomHashPair(Arrays.stream(new FCTUniversal(s).chars).filter(c -> c >= 0).toArray(), 100);
            assertNotNull(hash, s::toString);
        });
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
//            FixedCharTable.FCTRandomHashPair._p = candidates[i];
//            IntStream lengths = r.ints(5, 64);
//            StringBuilder sb = new StringBuilder(64);
//            int sum = lengths.limit(100000).mapToObj(l ->
//                            r.ints(' ', 'я').limit(l)
//                                    .collect(() -> {
//                                        sb.setLength(0);
//                                        return sb;
//                                    }, StringBuilder::appendCodePoint, StringBuilder::append)
//                    ).map(FixedCharTable::from)
//                    .mapToInt(in -> ((FixedCharTable.FCTRandomHashPair) in).clashCount)
//                    .sum();
//            System.out.println(candidates[i] + "\t" + sum);
//        }
//    }

}