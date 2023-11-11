package com.pe.text;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class IterativeMultiplePatternsTest {

    @ParameterizedTest
    @CsvSource({
            "test,test,0,4",
            "test,atest,1,5",
            "test,tetest,2,6",
    })
    void testFuzzy0(String test, String text, int start, int end) {
        MatcherProvider patterns = new IterativeMultiplePatterns(new IterativeFuzzyPattern[]{new Bitap65Plus(test, 0)});
        FuzzyMatcher matcher = patterns.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(0, matcher.distance());
        assertFalse(matcher.find());
    }

    @Test
    void testFuzzy0Fail() {
        String test = "test";
        String text = "aaaa";
        MatcherProvider patterns = new IterativeMultiplePatterns(new IterativeFuzzyPattern[]{new Bitap65Plus(test, 0)});
        FuzzyMatcher matcher = patterns.matcher(text);
        assertFalse(matcher.find());
        assertThrows(IllegalStateException.class, matcher::start);
    }

    @ParameterizedTest
    @CsvSource({
            "test,test,0,4,0",
            "test,tet,0,3,1",
            "test,tes,0,3,1",
            "test,tost,0,4,1",
            "test,tesl,0,4,1",
            "test,te5t,0,4,1",
            "test,_test,1,5,0",
            "test,_te5t,1,5,1",
            "test,tst,0,3,1",
            "test,_tst,1,4,1",
            "test,t_est,0,5,1",
            "test,tes_t,0,5,1"
    })
    void testFuzzy1(String test, String text, int start, int end, int d) {
        MatcherProvider patterns = new IterativeMultiplePatterns(new IterativeFuzzyPattern[]{new Bitap65Plus(test, 1)});
        FuzzyMatcher matcher = patterns.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(d, matcher.distance());
    }

    @ParameterizedTest
    @CsvSource({
            "test,__st",
            "test,to_t",
            "test,_esl",
            "test,_e5t",
            "test,_t_5t",
            "test,ts__",
            "test,_ts_",
            "test,t_es_",
            "test,_es_t"
    })
    void testFuzzy1Fail(String test, String text) {
        FuzzyMultiPattern patterns = new IterativeMultiplePatterns(new IterativeFuzzyPattern[]{new Bitap65Plus(test, 0)});
        FuzzyMatcher matcher = patterns.matcher(text);
        assertFalse(matcher.find());
    }

    @ParameterizedTest
    @CsvSource({
            "Result,Result,0,6,0",
            "Result,Resul,0,5,1",
            "Result,Resu,0,4,2",
            "Result,Resul_,0,6,1",
            "Result,Resu_t,0,6,1",
            "Result,_esult,0,6,1",
            "Result,_esul_,0,6,2",
            "Result,_esul_t,0,7,2",
            "Result,_Result,1,7,0",
            "Result,_Resul_,1,7,1",
            "Result,_Resu_t,1,7,1",
            "Result,__esult,1,7,1",
            "Result,__esul_,1,7,2",
            "Result,__esul_t,1,8,2"
    })
    void testFuzzy2(String test, String text, int start, int end, int d) {
        MatcherProvider patterns = new IterativeMultiplePatterns(new IterativeFuzzyPattern[]{new Bitap65Plus(test, 2)});
        FuzzyMatcher matcher = patterns.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(d, matcher.distance());
    }

    @Test
    void testAllMatches() {
        MatcherProvider patterns = new IterativeMultiplePatterns(new IterativeFuzzyPattern[]{new Bitap65Plus("test", 1)});
        String text = "Test string to test all matches. tes";
        FuzzyMatcher matcher = patterns.matcher(text);
        assertTrue(matcher.find());
        assertEquals(0, matcher.start());
        assertEquals(4, matcher.end());
        assertEquals(1, matcher.distance());

        assertTrue(matcher.find());
        int index = text.indexOf("test");
        assertEquals(index, matcher.start());
        assertEquals(index + 4, matcher.end());
        assertEquals(0, matcher.distance());

        assertTrue(matcher.find());
        assertEquals(text.length() - 3, matcher.start());
        assertEquals(text.length(), matcher.end());
        assertEquals(1, matcher.distance());

        assertFalse(matcher.find());
    }

    @Test
    void testLongPattern() {
        FuzzyMultiPattern patterns = FuzzyMultiPattern.combine(
                FuzzyPattern.pattern("ut", 0, true),
                FuzzyPattern.pattern("Duis", 1),
                FuzzyPattern.pattern("dolor", 1)
        );

        Stream<FuzzyResult> resultStream = patterns.matcher("Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
        ).stream();

        String results = resultStream.peek(System.out::println).map(FuzzyResult::foundText).collect(Collectors.joining(","));
        assertEquals("dolor,ut,dolor,Ut,quis,ut,Duis,ut,dolor,dolor", results);
    }


    @Test
    void testInvalidPatterns() {
        try {
            FuzzyMultiPattern matcher = FuzzyMultiPattern.combine(FuzzyPattern.pattern("1", 0), null);
            assertNull(matcher);
        } catch (IllegalArgumentException e) {
            assertEquals("2nd pattern is null", e.getMessage());
        }

        try {
            FuzzyMultiPattern matcher = FuzzyMultiPattern.combine(new FuzzyPattern() {
                @Override
                public CharSequence text() {
                    return null;
                }

                @Override
                public int maxLevenshteinDistance() {
                    return 0;
                }

                @Override
                public boolean caseInsensitive() {
                    return false;
                }

                @Override
                public FuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex) {
                    return null;
                }
            }, null);

            assertNull(matcher);

        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("2nd pattern is null"));
        }
    }

}