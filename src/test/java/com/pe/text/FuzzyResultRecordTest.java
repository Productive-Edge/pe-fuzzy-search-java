package com.pe.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FuzzyResultRecordTest {

    @Test
    void testToStringContainsClassName() {
        FuzzyPattern pattern = FuzzyPattern.compile("test", 1);
        FuzzyMatcher matcher = pattern.matcher("test");
        assertTrue(matcher.find(), "Matching should be found");
        FuzzyResultRecord record = new FuzzyResultRecord(matcher);
        String str = record.toString();
        assertTrue(str.startsWith("FuzzyResult{"), "toString should start with class name");
    }
}
