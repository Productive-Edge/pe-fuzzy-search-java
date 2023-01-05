package com.pe.juzzy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


public class BitapsBenchmark {

    private static final String TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
            "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
            "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
            "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    private static final String PATTERN ="dolore";

    @Disabled
    @Test
    public void runBenchmarks() throws Exception {
        new Runner(
                new OptionsBuilder()
                        .include(this.getClass().getName() + ".*")
                        .mode(Mode.AverageTime)
                        .warmupTime(TimeValue.seconds(1))
                        .warmupIterations(2)
                        .threads(2)
                        .measurementIterations(5)
                        .measurementTime(TimeValue.seconds(1))
                        .forks(1)
                        .shouldDoGC(true)
                        .build()
        ).run();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void benchmark64() {
        JuzzyMatcher matcher = new Bitap64(PATTERN, 1).matcher(TEXT);
        while (matcher.find()) {
            assertTrue(matcher.distance() <= 1);
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void benchmark32() {
        JuzzyMatcher matcher = new Bitap32(PATTERN, 1).matcher(TEXT);
        while (matcher.find()) {
            assertTrue(matcher.distance() <= 1);
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void benchmark64X() {
        JuzzyMatcher matcher = new UnlimitedBitap(PATTERN, 1).matcher(TEXT);
        while (matcher.find()) {
            assertTrue(matcher.distance() <= 1);
        }
    }
}
