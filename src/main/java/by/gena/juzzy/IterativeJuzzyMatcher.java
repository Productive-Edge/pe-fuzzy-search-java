package by.gena.juzzy;

interface IterativeJuzzyMatcher extends JuzzyMatcher {
    void setFrom(int startIndex);
    void setTo(int lastIndex);
    void resetState();
    boolean testNextSymbol();
    boolean testNextInsert(int iteration);
}
