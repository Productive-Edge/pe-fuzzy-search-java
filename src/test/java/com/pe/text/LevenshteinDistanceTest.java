package com.pe.text;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevenshteinDistanceTest {

    @ParameterizedTest
    @CsvSource({
            "test,tst",
            "test,tost",
            "test,toest",
    })
    @Deprecated
    void compareWithApache(String s1, String s2) {
        LevenshteinDistance apache = LevenshteinDistance.getDefaultInstance();
        assertEquals(1, apache.apply(s1, s2));
        assertEquals(1, apache.apply(s2, s1));
        assertEquals(1, Levenshtein.distance(s1, s2));
        assertEquals(1, Levenshtein.distance(s2, s1));
    }
}
