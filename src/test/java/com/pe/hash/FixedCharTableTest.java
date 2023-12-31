package com.pe.hash;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void testRandom() {
        Random r = new Random();
        IntStream lengths = r.ints(3, 64);
        StringBuilder sb = new StringBuilder(64);
        lengths.limit(100).mapToObj(l ->
                r.ints(' ', 'я').limit(l)
                        .collect(() -> {
                            sb.setLength(0);
                            return sb;
                        }, StringBuilder::appendCodePoint, StringBuilder::append)
        ).forEach(FixedCharTable::from);
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