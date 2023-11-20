package com.pe.text;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class Bitap64Test {

    @ParameterizedTest
    @CsvSource({
            "test,test,0,4",
            "test,atest,1,5",
            "test,tetest,2,6",
    })
    void testFuzzy0(String test, String text, int start, int end) {
        FuzzyPattern bitap = new Bitap64(test, 0);
        FuzzyMatcher matcher = bitap.matcher(text);
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
        FuzzyPattern bitap = new Bitap64(test, 0);
        FuzzyMatcher matcher = bitap.matcher(text);
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
        FuzzyPattern bitap = new Bitap64(test, 1);
        FuzzyMatcher matcher = bitap.matcher(text);
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
        FuzzyPattern bitap = new Bitap64(test, 1);
        FuzzyMatcher matcher = bitap.matcher(text);
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
        FuzzyPattern bitap = new Bitap64(test, 2);
        FuzzyMatcher matcher = bitap.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(d, matcher.distance());
    }

    @Test
    void testAllMatches() {
        FuzzyPattern bitap = new Bitap64("test", 1);
        String text = "Test string to test all matches. tes";
        FuzzyMatcher matcher = bitap.matcher(text);
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
    void testBestMatching() {
        final String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

        {
            FuzzyPattern dolore = new Bitap64("dolore", 1);
            FuzzyMatcher matcher = dolore.matcher(text);
            int i = 0;
            String[] results = new String[]{"dolor ", "dolore", "dolor ", "dolore"};
            while (matcher.find()) {
                assertEquals(results[i], text.subSequence(matcher.start(), matcher.end()));
                i++;
            }
            assertEquals(results.length, i);
        }
        {
            int i = 0;
            FuzzyPattern laboris = new Bitap64("laboris", 3);
            FuzzyMatcher matcher = laboris.matcher(text);
            String[] results = new String[]{"labore ", "laboris", "laborum"};
            while (matcher.find()) {
                assertEquals(results[i], text.subSequence(matcher.start(), matcher.end()));
                i++;
            }
            assertEquals(results.length, i);
        }
    }


    @ParameterizedTest
    @CsvSource({
            "test,Test,0,4,0",
            "Test,teT,0,3,1",
            "tEst,tEs,0,3,1",
            "teSt,tosT,0,4,1",
            "tesT,TESL,0,4,1",
            "TEst,Te5t,0,4,1",
            "tESt,_Test,1,5,0",
            "teST,_Te5t,1,5,1",
            "TESt,Tst,0,3,1",
            "tSET,_Tst,1,4,1",
            "TeSt,T_est,0,5,1",
            "tEsT,Tes_t,0,5,1"
    })
    void caseInsensitive(String test, String text, int start, int end, int d) {
        FuzzyPattern bitap = new Bitap64(test, 1, true);
        FuzzyMatcher matcher = bitap.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(d, matcher.distance());
    }

    @Test
    void testMaxLen() {
        FuzzyPattern pattern = FuzzyPattern.pattern("1234567890123456789012345678901234567890123456789012345678901234", 1);
        assertTrue(pattern instanceof Bitap64);
        //exact
        {
            List<FuzzyResult> results = pattern.matcher("01234567890123456789012345678901234567890123456789012345678901234567890")
                    .stream().collect(Collectors.toList());
            assertEquals(1, results.size());
            assertEquals(1, results.get(0).start());
            assertEquals(0, results.get(0).distance());
            assertEquals(65, results.get(0).end());
        }
        //replace
        {
            List<FuzzyResult> results = pattern.matcher("0123456789_12345678901234567890123456789012345678901234567890123456789")
                    .stream().collect(Collectors.toList());
            assertEquals(1, results.size());
            assertEquals(1, results.get(0).start());
            assertEquals(1, results.get(0).distance());
            assertEquals(65, results.get(0).end());
        }
        //better than replace
        {
            List<FuzzyResult> results = pattern.matcher("0123456789_123456789012345678901234567890123456789012345678901234567890123456789")
                    .stream().collect(Collectors.toList());
            assertEquals(1, results.size());
            assertEquals(11, results.get(0).start());
            assertEquals(0, results.get(0).distance());
            assertEquals(75, results.get(0).end());
        }
        //insert
        {
            List<FuzzyResult> results = pattern.matcher("0123456789123456789012345678901234567890123456789012345678901234567890")
                    .stream().collect(Collectors.toList());
            assertEquals(1, results.size());
            assertEquals(1, results.get(0).start());
            assertEquals(1, results.get(0).distance());
            assertEquals(64, results.get(0).end());
        }
        //delete
        {
            List<FuzzyResult> results = pattern.matcher("0123456789_0123456789012345678901234567890123456789012345678901234567890")
                    .stream().collect(Collectors.toList());
            assertEquals(1, results.size());
            assertEquals(1, results.get(0).start());
            assertEquals(1, results.get(0).distance());
            assertEquals(66, results.get(0).end());
        }
    }

    @Test
    void testEdgeCase() {
        FuzzyPattern p = new Bitap64("aabaa", 2);
        FuzzyMatcher m = p.matcher("aaa");
        assertTrue(m.find());
        assertEquals(0, m.start(), "start");
        assertEquals(3, m.end(), "end");
        assertEquals("aaa", m.foundText().toString(), "foundText");
        assertEquals(2, m.distance(), "distance");
        assertFalse(m.find());
    }

    @Test
    void testBestOnEdgeCases() {
        {
            FuzzyPattern p = new Bitap64("ababCabab", 2);
            FuzzyMatcher m = p.matcher("abababCababab");
            assertTrue(m.find());
            assertEquals(2, m.start(), "start");
            assertEquals(11, m.end(), "end");
            assertEquals("ababCabab", m.foundText().toString(), "foundText");
            assertEquals(0, m.distance(), "distance");
            assertFalse(m.find());
        }
        {
            FuzzyPattern p = new Bitap64("aabaa", 1);
            FuzzyMatcher m = p.matcher("aaabaaa");
            assertTrue(m.find());
            assertEquals(6, m.end(), "end");
            assertEquals(1, m.start(), "start");
            assertEquals("aabaa", m.foundText().toString(), "foundText");
            assertEquals(0, m.distance(), "distance");
            assertFalse(m.find());
        }
    }

    @Test
    void testAbcBy2a() {
        FuzzyPattern pattern = new Bitap64("aa", 1);
        FuzzyMatcher matcher = pattern.matcher("abc");
        assertTrue(matcher.find());
        assertEquals(0, matcher.start());
        assertEquals(1, matcher.end());
        assertEquals(1, matcher.distance());
        assertEquals("aa", matcher.pattern().text());
        assertFalse(matcher.find());
    }

    @Test
    void test1aBy2a() {
        FuzzyPattern pattern = new Bitap64("aa", 1);
        FuzzyMatcher matcher = pattern.matcher("a");
        assertTrue(matcher.find());
        assertEquals(0, matcher.start());
        assertEquals(1, matcher.end());
        assertEquals(1, matcher.distance());
        assertEquals("aa", matcher.pattern().text());
        assertFalse(matcher.find());
    }

    @Test
    void test3aBy2a() {
        FuzzyPattern pattern = new Bitap64("aa", 1);
        FuzzyMatcher matcher = pattern.matcher("aaa");
        assertTrue(matcher.find());
        assertEquals(0, matcher.start());
        assertEquals(2, matcher.end());
        assertEquals(0, matcher.distance());
        assertEquals("aa", matcher.pattern().text());
        assertTrue(matcher.find());
        assertEquals(2, matcher.start());
        assertEquals(3, matcher.end());
        assertEquals(1, matcher.distance());
        assertEquals("aa", matcher.pattern().text());
        assertFalse(matcher.find());
    }

    @Test
    void test5aBy2a() {
        FuzzyPattern pattern = new Bitap64("aa", 1);
        FuzzyMatcher matcher = pattern.matcher("aaaaa");
        assertTrue(matcher.find());
        assertEquals(0, matcher.start());
        assertEquals(2, matcher.end());
        assertEquals(0, matcher.distance());
        assertEquals("aa", matcher.pattern().text());
        assertTrue(matcher.find());
        assertEquals(2, matcher.start());
        assertEquals(4, matcher.end());
        assertEquals(0, matcher.distance());
        assertEquals("aa", matcher.pattern().text());
        assertTrue(matcher.find());
        assertEquals(4, matcher.start());
        assertEquals(5, matcher.end());
        assertEquals(1, matcher.distance());
        assertEquals("aa", matcher.pattern().text());
        assertFalse(matcher.find());
    }

    @Test
    void testRealCase() {
        String text = "4. Dental? [ ! Medicai? ] I (!f both, complete 3-11 for dental oniy.i";
        FuzzyPattern pattern = FuzzyPattern.pattern(" (if both, complete 5-11 for dental only.)", 10);
        assertEquals(
                " (!f both, complete 3-11 for dental oniy.i",
                pattern.matcher(text).findTheBest().map(FuzzyResult::foundText).orElse("")
        );

    }

}