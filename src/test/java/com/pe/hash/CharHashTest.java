package com.pe.hash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CharHashTest {

    @Test
    void testBitMask() {
        String s = "Sets Гена of testing below might not be final and in case not found increase mask"
                + "\r\n int[] chars = s.chars().sorted().distinct().toArray();";
        int[] chars = s.chars().sorted().distinct().toArray();
        FCTMinPerfHash hash = FCTMinPerfHash.findFor(chars);
        assertNotNull(hash);
        for (int i = 0; i < chars.length; i++) {
            assertEquals(i, hash.indexOf(chars[i]));
        }
    }


}