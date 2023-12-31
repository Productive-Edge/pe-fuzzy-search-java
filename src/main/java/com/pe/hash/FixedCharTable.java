package com.pe.hash;

interface FixedCharTable {

    static FixedCharTable from(CharSequence charSequence) {
        if (charSequence == null || charSequence.length() == 0) return FCT0.INSTANCE;
        int[] distinct = charSequence.chars().sorted().distinct().toArray();
        if (distinct.length == 1) return new FCT1(distinct[0]);
        if (distinct.length == 2) return new FCT2(distinct[0], distinct[1]);
        return FCTMinPerfHash.findFor(distinct);
//        return new FCTRandomHashPair(distinct);
    }

    int indexOf(char c);

    int size();


}
