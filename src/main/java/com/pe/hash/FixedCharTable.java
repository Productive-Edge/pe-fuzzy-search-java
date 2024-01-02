package com.pe.hash;

import java.util.Arrays;

interface FixedCharTable {

    static FixedCharTable from(CharSequence charSequence) {
        if (charSequence == null || charSequence.length() == 0) return FCT0.INSTANCE;
        FCTUniversal universal = new FCTUniversal(charSequence);
        int[] distinct = Arrays.stream(universal.chars).filter(c -> c >= 0).toArray();
        if (distinct.length == 1) return new FCT1(distinct[0]);
        if (distinct.length == 2) return new FCT2(distinct[0], distinct[1]);
        FCTRandomHashPair randomHashPair = new FCTRandomHashPair(distinct, 100);
        if (randomHashPair.found())
            return randomHashPair;
        Arrays.sort(distinct);
        FCTMinPerfHash minPerfHash = FCTMinPerfHash.findFor(distinct);
        if (minPerfHash == null)
            return universal;
        return minPerfHash;
    }

    int indexOf(char c);

    int size();


}
