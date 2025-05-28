package com.pe.hash;

import java.util.Arrays;

interface FixedCharTable {

    static FixedCharTable from(CharSequence charSequence) {
        if (charSequence == null || charSequence.length() == 0) return FCT0.INSTANCE;
        FCTUniversal universal = new FCTUniversal(charSequence);
        int[] chars = universal.chars;
        int count = 0;
        for (int c : chars) {
            if (c >= 0) count++;
        }
        int[] distinct = new int[count];
        int i = 0;
        for (int c : chars) {
            if (c >= 0) distinct[i++] = c;
        }
        if (distinct.length == 1) return new FCT1(distinct[0]);
        if (distinct.length == 2) return new FCT2(distinct[0], distinct[1]);
        FCTCuckoo randomHashPair = new FCTCuckoo(distinct, 100);
        if (randomHashPair.found())
            return randomHashPair;
        return universal;
    }

    int indexOf(char c);

    int size();


}
