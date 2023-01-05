package com.pe.juzzy;

public interface JuzzyResult {
    JuzzyPattern pattern();
    int start();
    int end();
    int distance();
    CharSequence foundText();
}
