package by.gena.juzzy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static by.gena.juzzy.JuzzyPattern.oneOf;
import static by.gena.juzzy.JuzzyPattern.pattern;
import static org.junit.jupiter.api.Assertions.*;

class MultiplePatternsTest {

    @ParameterizedTest
    @CsvSource({
            "test,test,0,4",
            "test,atest,1,5",
            "test,tetest,2,6",
    })
    public void testFuzzy0(String test, String text, int start, int end) {
        MatcherProvider patterns = new MultiplePatterns(new IterativeJuzzyPattern[]{new UnlimitedBitap(test, 0)});
        JuzzyMatcher matcher = patterns.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(0, matcher.distance());
        assertFalse(matcher.find());
    }

    @Test
    public void testFuzzy0Fail() {
        String test = "test";
        String text = "aaaa";
        MatcherProvider patterns = new MultiplePatterns(new IterativeJuzzyPattern[]{new UnlimitedBitap(test, 0)});
        JuzzyMatcher matcher = patterns.matcher(text);
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
            "test,tes_t,0,4,1" // replacement _ -> t
    })
    public void testFuzzy1(String test, String text, int start, int end, int d) {
        MatcherProvider patterns = new MultiplePatterns(new IterativeJuzzyPattern[]{new UnlimitedBitap(test, 1)});
        JuzzyMatcher matcher = patterns.matcher(text);
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
    public void testFuzzy1Fail(String test, String text) {
        MatcherProvider patterns = new MultiplePatterns(new IterativeJuzzyPattern[]{new UnlimitedBitap(test, 0)});
        JuzzyMatcher matcher = patterns.matcher(text);
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
            "Result,_esul_t,0,6,2",
            "Result,_Result,1,7,0",
            "Result,_Resul_,1,7,1",
            "Result,_Resu_t,1,7,1",
            "Result,__esult,1,7,1",
            "Result,__esul_,1,7,2",
            "Result,__esul_t,1,7,2"
    })
    public void testFuzzy2(String test, String text, int start, int end, int d) {
        MatcherProvider patterns = new MultiplePatterns(new IterativeJuzzyPattern[]{new UnlimitedBitap(test, 2)});
        JuzzyMatcher matcher = patterns.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(d, matcher.distance());
    }

    @Test
    public void testAllMatches() {
        MatcherProvider patterns = new MultiplePatterns(new IterativeJuzzyPattern[]{new UnlimitedBitap("test", 1)});
        String text = "Test string to test all matches. tes";
        JuzzyMatcher matcher = patterns.matcher(text);
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
    public void testLongPattern() {
        MatcherProvider patterns = JuzzyPattern.oneOf(
                pattern("ut", 0, true),
                pattern("Duis", 1),
                pattern("dolor", 1)
        );

        Stream<JuzzyResult> resultStream = patterns.streamMatches("Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");

        String results = resultStream.map(JuzzyResult::foundText).collect(Collectors.joining(","));
        assertEquals("dolor,ut,dolor,Ut,quis,ut,Duis,ut,dolor,dolor", results);
    }


    @Test
    public void testInvalidPatterns() {
        try {
            MatcherProvider matcher = oneOf(pattern("1", 0), null);
            assertNull(matcher);
        }catch (IllegalArgumentException e) {
            assertEquals("2nd pattern is null", e.getMessage());
        }

        try {
            MatcherProvider matcher = oneOf(new JuzzyPattern() {
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
                public JuzzyMatcher matcher(CharSequence text, int fromIndex, int toIndex) {
                    return null;
                }
            }, null);

            assertNull(matcher);

        }catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("1st pattern"));
            assertTrue(e.getMessage().contains(" is not supported, since it doesn't implement "));
        }

    }
}