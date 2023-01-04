package by.gena.juzzy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class UnlimitedBitapTest {

    @ParameterizedTest
    @CsvSource({
            "test,test,0,4",
            "test,atest,1,5",
            "test,tetest,2,6",
    })
    public void testFuzzy0(String test, String text, int start, int end) {
        JuzzyPattern bitap = new UnlimitedBitap(test, 0);
        JuzzyMatcher matcher = bitap.matcher(text);
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
        JuzzyPattern bitap = new UnlimitedBitap(test, 0);
        JuzzyMatcher matcher = bitap.matcher(text);
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
        JuzzyPattern bitap = new UnlimitedBitap(test, 1);
        JuzzyMatcher matcher = bitap.matcher(text);
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
        JuzzyPattern bitap = new UnlimitedBitap(test, 1);
        JuzzyMatcher matcher = bitap.matcher(text);
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
        JuzzyPattern bitap = new UnlimitedBitap(test, 2);
        JuzzyMatcher matcher = bitap.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(d, matcher.distance());
    }

    @Test
    public void testAllMatches() {
        JuzzyPattern bitap = new UnlimitedBitap("test", 1);
        String text = "Test string to test all matches. tes";
        JuzzyMatcher matcher = bitap.matcher(text);
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
    public void testBestMatching() {
        final String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

        {
            JuzzyPattern dolore = new UnlimitedBitap("dolore", 1);
            JuzzyMatcher matcher = dolore.matcher(text);
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
            JuzzyPattern laboris = new UnlimitedBitap("laboris", 3);
            JuzzyMatcher matcher = laboris.matcher(text);
            String[] results = new String[]{"labore ", "laboris", "laborum"};
            while (matcher.find()) {
                assertEquals(results[i], text.subSequence(matcher.start(), matcher.end()));
                i++;
            }
            assertEquals(results.length, i);
        }
    }

    @Test
    public void testAbnormal() {
        {
            JuzzyMatcher matcher = new UnlimitedBitap("aba", 1).matcher("aaba");
            while (matcher.find()) {
                System.out.println(matcher.foundText());
            }
        }
        {
            JuzzyMatcher matcher = new UnlimitedBitap("aba", 1).matcher("baba");
            int start = 0;
            while (matcher.find(start)) {
                System.out.println(matcher.foundText());
                start = matcher.end() - matcher.distance();
            }
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
            "tEsT,Tes_t,0,4,1" // replacement _ -> t
    })
    public void caseInsensitive(String test, String text, int start, int end, int d) {
        JuzzyPattern bitap = new UnlimitedBitap(test, 1, true);
        JuzzyMatcher matcher = bitap.matcher(text);
        assertTrue(matcher.find());
        assertEquals(start, matcher.start());
        assertEquals(end, matcher.end());
        assertEquals(d, matcher.distance());
    }

    @Test
    public void testLongPattern() {
        JuzzyPattern pattern = JuzzyPattern.pattern(
                "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat",
                5, true);

        JuzzyMatcher matcher = pattern.matcher("Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");

        while (matcher.find()) {
            assertEquals(0, matcher.distance());
            assertEquals(pattern.text(), matcher.foundText().toString());
        }
    }

}