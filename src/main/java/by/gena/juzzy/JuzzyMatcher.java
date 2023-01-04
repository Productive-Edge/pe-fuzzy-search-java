package by.gena.juzzy;

public interface JuzzyMatcher extends JuzzyResult {
    boolean find();
    boolean find(int fromIndex);
    boolean find(int fromIndex, int toIndex);
}
