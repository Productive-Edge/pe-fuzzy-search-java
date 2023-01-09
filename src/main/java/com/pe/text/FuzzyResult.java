package com.pe.text;

public interface FuzzyResult {
    FuzzyPattern pattern();
    int start();
    int end();
    int distance();
    CharSequence foundText();
}
