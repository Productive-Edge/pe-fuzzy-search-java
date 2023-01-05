package by.gena.juzzy;

interface IterativeJuzzyMatcher extends JuzzyMatcher {
    void resetState();
    int getMaxDistance();
    void setMaxDistance(int maxDistance);
    boolean testNextSymbol();

    boolean testNextInsert(int iteration);

    void improveResult(int maxIndex);
}
