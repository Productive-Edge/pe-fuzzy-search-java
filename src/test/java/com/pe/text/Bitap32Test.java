package com.pe.text;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class Bitap32Test {

    @Test
    void testNoDistance() {
        FuzzyPattern pattern = FuzzyPattern.compile("123", 0);
        FuzzyMatcher matcher = pattern.matcher("0123");
        assertTrue(matcher.find());
        assertEquals(0, matcher.distance());
        assertEquals("123", matcher.foundText());
        assertEquals(1, matcher.start());
        assertEquals(4, matcher.end());
    }

    @Test
    void testFullDistance() {
        FuzzyPattern pattern = FuzzyPattern.compile("12", 2);
        FuzzyMatcher matcher = pattern.matcher("0123");
        assertTrue(matcher.find());
        assertEquals(0, matcher.distance());
        assertEquals("12", matcher.foundText());
        assertEquals(1, matcher.start());
        assertEquals(3, matcher.end());
        assertFalse(matcher.streamEditTypes().findAny().isPresent());
    }

    @ParameterizedTest
    @CsvSource({
            "test,tst,0,3,'0,1'",
            "test,tost,0,4,'0,0'",
            "test,toest,0,5,'0,-1'",
    })
    void testOperations1(String test, String text, int start, int end, @ConvertWith(CsvIntsConverter.class) int[] changes) {
        FuzzyPattern bitap = new Bitap32(test, 1);
        FuzzyMatcher matcher = bitap.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(1, matcher.distance());
        assertArrayEquals(changes, ((BaseBitap.Matcher) matcher).lengthChanges);
        assertFalse(matcher.find());
    }

    @ParameterizedTest
    @CsvSource({
            "test,tst,0,3,'0,1,0'",
            "test,tost,0,4,'0,0,0'",
            "test,toest,0,5,'0,-1,0'",
    })
    void testOperations2(String test, String text, int start, int end, @ConvertWith(CsvIntsConverter.class) int[] changes) {
        FuzzyPattern bitap = new Bitap32(test, 2);
        FuzzyMatcher matcher = bitap.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(1, matcher.distance());
        assertArrayEquals(changes, ((BaseBitap.Matcher) matcher).lengthChanges);
        assertFalse(matcher.find());
    }

    @ParameterizedTest
    @CsvSource({
            "test,test,0,4",
            "test,atest,1,5",
            "test,tetest,2,6",
    })
    void testFuzzy0(String test, String text, int start, int end) {
        FuzzyPattern bitap = new Bitap32(test, 0);
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
        FuzzyPattern bitap = new Bitap32(test, 0);
        FuzzyMatcher matcher = bitap.matcher(text);
        assertFalse(matcher.find());
        assertThrows(IllegalStateException.class, matcher::start);
    }

    @ParameterizedTest
    @CsvSource({
            "test,0,4,0,'0,0'",
            "tet,0,3,1,'0,1'",
            "tes,0,3,1,'0,1'",
            "tost,0,4,1,'0,0'",
            "tesl,0,4,1,'0,0'",
            "te5t,0,4,1,'0,0'",
            "_test,1,5,0,'0,0'",
            "_te5t,1,5,1,'0,0'",
            "tst,0,3,1,'0,1'",
            "_tst,1,4,1,'0,1'",
            "t_est,0,5,1,'0,-1'",
            "tes_t,0,5,1,'0,-1'"
    })
    void testFuzzy1(String text, int start, int end, int d, @ConvertWith(CsvIntsConverter.class) int[] changes) {
        FuzzyPattern bitap = new Bitap32("test", 1);
        FuzzyMatcher matcher = bitap.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(d, matcher.distance());
        assertArrayEquals(changes, ((BaseBitap.Matcher) matcher).lengthChanges);
        assertFalse(matcher.find());
    }


    @ParameterizedTest
    @CsvSource({
            "Rsulut,0,6,2,'0,1,-1'",
            "Rsuult,0,6,2,'0,1,-1'",
            "Result,0,6,0,'0,0,0'",
            "Resul,0,5,1,'0,1,0'",
            "Resu,0,4,2,'0,1,1'",
            "Resul_,0,6,1,'0,0,0'",
            "Resu_t,0,6,1,'0,0,0'",
            "_esult,0,6,1,'0,0,0'",
            "_esul_,0,6,2,'0,0,0'",
            "_esul_t,0,7,2,'0,0,-1'",
            "_Result,1,7,0,'0,0,0'",
            "_Resul_,1,7,1,'0,0,0'",
            "_Resu_t,1,7,1,'0,0,0'",
            "__esult,1,7,1,'0,0,0'",
            "__esul_,1,7,2,'0,0,0'",
            "__esul_t,1,8,2,'0,0,-1'",
            "Resu__lt,0,8,2,'0,-1,-1'",
            "Res__ult,0,8,2,'0,-1,-1'",
    })
    void testFuzzy2(String text, int start, int end, int d, @ConvertWith(CsvIntsConverter.class) int[] changes) {
        FuzzyPattern bitap = new Bitap32("Result", 2);
        FuzzyMatcher matcher = bitap.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(d, matcher.distance());
        assertArrayEquals(changes, ((BaseBitap.Matcher) matcher).lengthChanges);
        assertFalse(matcher.find());
    }

    @ParameterizedTest
    @CsvSource({
            "__st",
            "to_t",
            "_esl",
            "_e5t",
            "_t_5t",
            "ts__",
            "_ts_",
            "t_es_",
            "_es_t"
    })
    void testFuzzy1Fail(String text) {
        FuzzyPattern bitap = new Bitap32("test", 1);
        FuzzyMatcher matcher = bitap.matcher(text);
        assertFalse(matcher.find());
    }

    @Test
    void testAllMatches() {
        FuzzyPattern bitap = new Bitap32("test", 1);
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
    void testBestMatchingEdgeCase() {
        FuzzyPattern laboris = new Bitap32("laboris", 2);
        FuzzyMatcher matcher = laboris.matcher("ut labore et");
        assertTrue(matcher.find());
        assertEquals("labore ", matcher.foundText());
    }

    @Test
    void testBestMatching() {
        final String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

        {
            int i = 0;
            FuzzyPattern laboris = new Bitap32("laboris", 2);
            FuzzyMatcher matcher = laboris.matcher(text);
            String[] results = new String[]{"labore ", "laboris", "laborum"};
            while (matcher.find()) {
                assertEquals(results[i], matcher.foundText());
                i++;
            }
            assertEquals(results.length, i);
        }
        {
            FuzzyPattern dolore = new Bitap32("dolore", 1);
            FuzzyMatcher matcher = dolore.matcher(text);
            int i = 0;
            String[] results = new String[]{"dolor ", "dolore", "dolor ", "dolore"};
            while (matcher.find()) {
                assertEquals(results[i], matcher.foundText());
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
        FuzzyPattern bitap = new Bitap32(test, 1, true);
        FuzzyMatcher matcher = bitap.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(d, matcher.distance());
    }

    @Test
    void testMaxLen() {
        FuzzyPattern pattern = FuzzyPattern.compile("12345678901234567890123456789012", 1);
        assertTrue(pattern instanceof Bitap32);
        //insert 2
        {
            List<FuzzyResult> results = pattern.matcher("0234567890123456789012345678901234567890")
                    .stream().collect(Collectors.toList());
            assertEquals(1, results.size());
            assertEquals(0, results.get(0).start());
            assertEquals(1, results.get(0).distance());
            assertEquals(32, results.get(0).end());
        }
        //insert
        {
            List<FuzzyResult> results = pattern.matcher("0123456789123456789012345678901234567890")
                    .stream().collect(Collectors.toList());
            assertEquals(1, results.size());
            assertEquals(1, results.get(0).start());
            assertEquals(1, results.get(0).distance());
            assertEquals(32, results.get(0).end());
        }
        //exact
        {
            List<FuzzyResult> results = pattern.matcher("01234567890123456789012345678901234567890")
                    .stream().collect(Collectors.toList());
            assertEquals(1, results.size());
            assertEquals(1, results.get(0).start());
            assertEquals(0, results.get(0).distance());
            assertEquals(33, results.get(0).end());
        }
        //replace
        {
            List<FuzzyResult> results = pattern.matcher("0123456789_123456789012345678901234567890")
                    .stream().collect(Collectors.toList());
            assertEquals(1, results.size());
            assertEquals(1, results.get(0).start());
            assertEquals(1, results.get(0).distance());
            assertEquals(33, results.get(0).end());
        }
        //delete
        {
            List<FuzzyResult> results = pattern.matcher("0123456789_0123456789012345678901234567890")
                    .stream().collect(Collectors.toList());
            assertEquals(1, results.size());
            assertEquals(1, results.get(0).start());
            assertEquals(1, results.get(0).distance());
            assertEquals(34, results.get(0).end());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "c",
            "b",
            "a"
    })
    void testEdgeCaseS3x1(String text) {
        FuzzyPattern p = new Bitap32("abc", 2);
        FuzzyMatcher m = p.matcher(text);
        assertTrue(m.find());
        assertEquals(0, m.start(), "start");
        assertEquals(1, m.end(), "end");
        assertEquals(text, m.foundText().toString(), "foundText");
        assertEquals(2, m.distance(), "distance");
        assertFalse(m.find());
    }

    @Test
    void testEdgeCaseS3x2() {
        FuzzyPattern p = new Bitap32("abc", 2);
        {
            FuzzyMatcher m = p.matcher("ac");
            assertTrue(m.find());
            assertEquals("ac", m.foundText().toString(), "foundText");
            assertEquals(0, m.start(), "start");
            assertEquals(2, m.end(), "end");
            assertEquals(1, m.distance(), "distance");
            assertFalse(m.find());
        }
    }

    @Test
    void testEdgeCaseS3x3() {
        FuzzyPattern p = new Bitap32("abc", 2);
        FuzzyMatcher m = p.matcher("acc");
        assertTrue(m.find());
        assertEquals("acc", m.foundText().toString(), "foundText");
        assertEquals(0, m.start(), "start");
        assertEquals(3, m.end(), "end");
        assertEquals(1, m.distance(), "distance");
        assertFalse(m.find());
    }

    @Test
    void testTwoReplacesInRow() {
        FuzzyPattern p = new Bitap32("abcd", 2);
        FuzzyMatcher m = p.matcher("addd");
        assertTrue(m.find());
        assertEquals("addd", m.foundText().toString(), "foundText");
        assertEquals(0, m.start(), "start");
        assertEquals(4, m.end(), "end");
        assertEquals(2, m.distance(), "distance");
        assertFalse(m.find());
    }

    @Test
    void testEdgeCaseS3x3u() {
        FuzzyPattern p = new Bitap32("abc", 2);
        FuzzyMatcher m = p.matcher("axc");
        assertTrue(m.find());
        assertEquals("axc", m.foundText().toString(), "foundText");
        assertEquals(0, m.start(), "start");
        assertEquals(3, m.end(), "end");
        assertEquals(1, m.distance(), "distance");
        assertFalse(m.find());
    }

    @Test
    void testEdgeCase2() {
        FuzzyPattern p = new Bitap32(" abc ", 2);
        FuzzyMatcher m = p.matcher(" AbC ");
        assertTrue(m.find());
        assertEquals(0, m.start(), "start");
        assertEquals(5, m.end(), "end");
        assertEquals(" AbC ", m.foundText().toString(), "foundText");
        assertEquals(2, m.distance(), "distance");
        assertFalse(m.find());
    }

    @Test
    void testEdgeCase3a() {
        FuzzyPattern p = new Bitap32("  abc  ", 3);
        FuzzyMatcher m = p.matcher("  AbC  ");
        assertTrue(m.find());
        assertEquals(0, m.start(), "start");
        assertEquals(7, m.end(), "end");
        assertEquals("  AbC  ", m.foundText().toString(), "foundText");
        assertEquals(2, m.distance(), "distance");
        assertFalse(m.find());
    }

    @Test
    void testEdgeCase3b() {
        FuzzyPattern p = new Bitap32(" _abc_ ", 2);
        FuzzyMatcher m = p.matcher(" _AbC_ ");
        assertTrue(m.find());
        assertEquals(0, m.start(), "start");
        assertEquals(7, m.end(), "end");
        assertEquals(" _AbC_ ", m.foundText().toString(), "foundText");
        assertEquals(2, m.distance(), "distance");
        assertFalse(m.find());
    }

    @Test
    void testEdgeCase3c() {
        FuzzyPattern p = new Bitap32(" _abc  ", 2);
        FuzzyMatcher m = p.matcher(" _AbC  ");
        assertTrue(m.find());
        assertEquals(0, m.start(), "start");
        assertEquals(7, m.end(), "end");
        assertEquals(" _AbC  ", m.foundText().toString(), "foundText");
        assertEquals(2, m.distance(), "distance");
        assertFalse(m.find());
    }

    @Test
    void testEdgeCase2a() {
        FuzzyPattern p = new Bitap32("_abc ", 2);
        FuzzyMatcher m = p.matcher("_AbC ");
        assertTrue(m.find());
        assertEquals(0, m.start(), "start");
        assertEquals(5, m.end(), "end");
        assertEquals("_AbC ", m.foundText().toString(), "foundText");
        assertEquals(2, m.distance(), "distance");
        assertFalse(m.find());
    }

    @Test
    void testEdgeCase3() {
        FuzzyPattern p = new Bitap32(" abbc ", 2);
        FuzzyMatcher m = p.matcher(" AbbC ");
        assertTrue(m.find());
        assertEquals(0, m.start(), "start");
        assertEquals(6, m.end(), "end");
        assertEquals(" AbbC ", m.foundText().toString(), "foundText");
        assertEquals(2, m.distance(), "distance");
        assertFalse(m.find());
    }

    @Test
    void testEdgeCase4() {
        FuzzyPattern p = new Bitap32(" abbc", 2);
        FuzzyMatcher m = p.matcher(" AbbC");
        assertTrue(m.find());
        assertEquals(0, m.start(), "start");
        assertEquals(5, m.end(), "end");
        assertEquals(" AbbC", m.foundText().toString(), "foundText");
        assertEquals(2, m.distance(), "distance");
        assertFalse(m.find());
    }


    @Test
    void testEdgeCaseSR() {
        FuzzyPattern p = new Bitap32("cba", 2);
        FuzzyMatcher m = p.matcher("a");
        assertTrue(m.find());
        assertEquals(0, m.start(), "start");
        assertEquals(1, m.end(), "end");
        assertEquals("a", m.foundText().toString(), "foundText");
        assertEquals(2, m.distance(), "distance");
        assertFalse(m.find());
    }

    @Test
    void testEdgeCase() {
        FuzzyPattern p = new Bitap32("aabaa", 2);
        FuzzyMatcher m = p.matcher("aaa");
        assertTrue(m.find());
        assertEquals(0, m.start(), "start");
        assertEquals(3, m.end(), "end");
        assertEquals("aaa", m.foundText().toString(), "foundText");
        assertEquals(2, m.distance(), "distance");
        assertArrayEquals(
                new OperationType[]{OperationType.INSERTION, OperationType.INSERTION},
                m.streamEditTypes().toArray());
        List<Operation> all = m.streamCharByCharOperations().collect(Collectors.toList());
        assertEquals(5, all.size());

        assertEquals(OperationType.MATCHING, all.get(0).type());
        assertNotNull(all.get(0).patternChar());
        assertEquals('a', all.get(0).patternChar().value());
        assertEquals(0, all.get(0).patternChar().index());
        assertNotNull(all.get(0).textChar());
        assertEquals('a', all.get(0).textChar().value());
        assertEquals(0, all.get(0).textChar().index());

        assertEquals(OperationType.MATCHING, all.get(1).type());
        assertNotNull(all.get(1).patternChar());
        assertEquals('a', all.get(1).patternChar().value());
        assertEquals(1, all.get(1).patternChar().index());
        assertNotNull(all.get(1).textChar());
        assertEquals('a', all.get(1).textChar().value());
        assertEquals(1, all.get(1).textChar().index());

        assertEquals(OperationType.INSERTION, all.get(2).type());
        assertNotNull(all.get(2).patternChar());
        assertEquals('b', all.get(2).patternChar().value());
        assertEquals(2, all.get(2).patternChar().index());
        assertNull(all.get(2).textChar());


        assertEquals(OperationType.MATCHING, all.get(3).type());
        assertNotNull(all.get(3).patternChar());
        assertEquals('a', all.get(3).patternChar().value());
        assertEquals(3, all.get(3).patternChar().index());
        assertNotNull(all.get(3).textChar());
        assertEquals('a', all.get(3).textChar().value());
        assertEquals(2, all.get(3).textChar().index());

        assertEquals(OperationType.INSERTION, all.get(4).type());
        assertNotNull(all.get(4).patternChar());
        assertEquals('a', all.get(4).patternChar().value());
        assertEquals(4, all.get(4).patternChar().index());
        assertNull(all.get(4).textChar());

        assertFalse(m.find());
    }

    @Test
    void testBestOnEdgeCases() {
        {
            FuzzyPattern p = new Bitap32("ababCabab", 2);
            FuzzyMatcher m = p.matcher("abababCababab");
            assertTrue(m.find());
            assertEquals(2, m.start(), "start");
            assertEquals(11, m.end(), "end");
            assertEquals("ababCabab", m.foundText().toString(), "foundText");
            assertEquals(0, m.distance(), "distance");
            assertFalse(m.find());
        }
        {
            FuzzyPattern p = new Bitap32("aabaa", 1);
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
        FuzzyPattern pattern = FuzzyPattern.compile("aa", 1);
        FuzzyMatcher matcher = pattern.matcher("abc");
        assertTrue(matcher.find());
        assertEquals(0, matcher.start());
        assertEquals(2, matcher.end());
        assertEquals(1, matcher.distance());
        assertEquals("ab", matcher.foundText());
        List<Operation> all = matcher.streamCharByCharOperations().collect(Collectors.toList());
        assertEquals(2, all.size());
        Assertions.assertEquals(OperationType.MATCHING, all.get(0).type());
        Assertions.assertEquals(OperationType.REPLACEMENT, all.get(1).type());
        Assertions.assertEquals('a', all.get(1).patternChar().value());
        Assertions.assertEquals('b', all.get(1).textChar().value());
        Assertions.assertEquals(0.5f, matcher.similarity());
        assertFalse(matcher.find());
    }

    @Test
    void test1aBy2a() {
        FuzzyPattern pattern = FuzzyPattern.compile("aa", 1);
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
        FuzzyPattern pattern = FuzzyPattern.compile("aa", 1);
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
        FuzzyPattern pattern = FuzzyPattern.compile("aa", 1);
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
    @Disabled("this code is just for README.md")
    void readme() {
        FuzzyPattern
                .compile("Medical?", 3)
                .matcher("4. Dental? [ ! Medicai? ] I (!f both, complete 3-11 for dental oniy.i")
                .stream()
                .forEach(System.out::println);
        assertTrue(true);
    }

    @Test
    void testRealCase() {
        String text = "4. Dental? [ ! Medicai? ] I (!f both, complete 3-11 for dental oniy.i";
        FuzzyPattern pattern = FuzzyPattern.compile(" (if both, complete 5-11 for ", 10);
        assertEquals(
                " (!f both, complete 3-11 for ",
                pattern.matcher(text).findTheBest().map(FuzzyResult::foundText).orElse("")
        );
    }

    @Test
    void testTheBest() {
        String text = "AABAAA";
        FuzzyPattern pattern = FuzzyPattern.compile("AAA", 2);
        assertEquals("AABA", pattern.matcher(text).findTheBest().map(FuzzyResult::foundText).orElse(null));
        assertEquals("AAA", pattern.matcher(text).findTheBest(true).map(FuzzyResult::foundText).orElse(null));
    }

    @Test
    void testRepeated() {
        String text = "b aa ba a a a";
        String ptrn = "a aa aaa a";
        FuzzyPattern pattern = FuzzyPattern.compile(ptrn, 6);
        BaseBitap.Matcher matcher = (BaseBitap.Matcher) pattern.matcher(text);
        assertTrue(matcher.find());
        matcher.streamEditsDetails().forEach(System.out::println);
        assertEquals(2, matcher.distance());
        assertEquals("a ba a a a", matcher.foundText());
        assertArrayEquals(new int[]{0, 0, 0, 0, 0, 0, 0}, matcher.lengthChanges);
    }


    @ParameterizedTest
    @CsvSource({
            "insert replace delete,nsert rrplace ddelete,3",
            "insert replace delete re,nsert rrplace ddelete rr,4",
            "insert replace delete dde,nsert rrplace ddelete de,4",
    })
    void insertionBeforeReplacementAndDeletion(String pattern, String text, int maxDiff) {
        FuzzyMatcher matcher = FuzzyPattern.compile(pattern, maxDiff)
                .matcher(text);
        assertTrue(matcher.find());
        assertEquals(text, matcher.foundText());
        List<Operation> edits = matcher.streamEditsDetails().collect(Collectors.toList());
        assertEquals(maxDiff, edits.size());
        assertEquals(OperationType.INSERTION, edits.get(0).type());
        assertEquals(OperationType.REPLACEMENT, edits.get(1).type());
        assertEquals(OperationType.DELETION, edits.get(2).type());
    }

    @Test
    void testMissingS() {
        FuzzyMatcher matcher = FuzzyPattern.compile("3 Mis Teeth", 4)
                .matcher("3 Mis    Teeth Information         {Place an X         on each missing toofh.)");
        assertTrue(matcher.find());
        assertEquals("3 Mis    Teeth", matcher.foundText());
    }

    @ParameterizedTest
    @CsvSource({
            "33. Missing Teeth Information",
            "33. Missing  Teeth Information",
            "33. Missing   Teeth Information",
            "33. Missing    Teeth Information",
            "33. Missing     Teeth Information",
            "33. Missing      Teeth Information",
            "33. Missing       Teeth Information",
            "33. Missing        Teeth Information",
            "33. Missing x Teeth Information",
            "33. Missing xx Teeth Information",
            "33. Missing xxx Teeth Information",
            "33. Missing xxxx Teeth Information",
            "33. Missing xxxxx Teeth Information",
            "33. Missing xxxxxx Teeth Information",
            "33. Missing xxxxxxx Teeth Information",
            "33. Missing T Teeth Information",
            "33. Missing Te Teeth Information",
            "33. Missing Tee Teeth Information",
            "33. Missing Teet Teeth Information",
            "33. Missing Teeth Teeth Information", // was failing on maxLeviD = 10, but worked on 9, due optimistic improvements
            "33. Missing Teeth  Teeth Information",
    })
    void testMissing(String text) {
        FuzzyMatcher matcher = FuzzyPattern.compile("33. Missing Teeth Information", 10)
                .matcher(text);
        assertTrue(matcher.find());
        System.out.println(matcher.streamEditTypes().map(Enum::toString).collect(Collectors.joining(",")));
        assertEquals(text, matcher.foundText());
    }

    @ParameterizedTest
    @CsvSource({
            "1234567890,1234567890",
            "1_234567890,1_234567890",
            "1__234567890,_234567890",
            "1___234567890,_234567890",
    })
    void testDeletions(String text, String expected) {
        FuzzyMatcher matcher = FuzzyPattern.compile("1234567890", 9)
                .matcher(text);
        assertTrue(matcher.find());
        System.out.println(matcher.streamEditTypes().map(Enum::toString).collect(Collectors.joining(",")));
        assertEquals(expected, matcher.foundText());
    }


}