package by.gena.juzzy;

interface IterativeJuzzyMatcher extends JuzzyMatcher {
    void setEnd(int atIndex);
    void resetState();
    boolean testNextSymbol();
    boolean testNextInsert(int iteration);
}
