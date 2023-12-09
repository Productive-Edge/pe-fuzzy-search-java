package com.pe.hash;

import org.junit.jupiter.api.Test;

class CharHashTest {


    @Test
    void testBitMask() {
        String s = "Sets Гена";
//        s.chars().forEach(c -> System.out.println("" + ((char) c) + ' ' + Integer.toBinaryString(~c)));
//        int ors = s.chars().reduce(-1, (r, c) -> r & ~c);
//        System.out.println("|:" + Integer.toBinaryString(ors));
//        int ands = s.chars().reduce(0, (r, c) -> r | ~c);
//        System.out.println("&:" + Integer.toBinaryString(ands));
//        System.out.println("^:" + Integer.toBinaryString(~(ands ^ ors)));
        s.chars().forEach(c -> System.out.println("~" + ((char) c) + ' ' + Integer.toBinaryString(~c)));
        int o = s.chars().reduce(0, (r, c) -> r | c);
        int a = s.chars().reduce(-1, (r, c) -> r & c);
        int x = o ^ a;
        System.out.println("~| " + Integer.toBinaryString(~o));
        System.out.println("~& " + Integer.toBinaryString(~a));
        System.out.println("~^ " + Integer.toBinaryString(~x));
        int[] counts = new int[16];
        int bit = 0;
        while (x != 0) {
            counts[bit] = bit << 16;
            if ((x & 1) != 0) {

            }
            bit++;
            x >>>= 1;
        }
    }


}