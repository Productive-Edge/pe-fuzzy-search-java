package com.pe.text;

interface IterativeFuzzyMatcher extends FuzzyMatcher {
    void resetState();
    int getMaxDistance();
    void setMaxDistance(int maxDistance);
    boolean testNextSymbol();

    boolean testNextInsert(int iteration);

    void improveResult(int maxIndex);
    void setIndex(int index);
}
